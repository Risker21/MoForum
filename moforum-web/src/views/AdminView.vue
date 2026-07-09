<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAdminStats, type AdminStats } from '@/api/admin'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<AdminStats | null>(null)
const loading = ref(true)

onMounted(async () => {
  if (userStore.username !== 'admin') {
    ElMessage.error('无权限')
    router.push({ name: 'home' })
    return
  }
  try {
    const { data } = await getAdminStats()
    if (data.success) {
      stats.value = data
      await nextTick()
      requestAnimationFrame(() => initCharts(data))
    }
  } catch {
    ElMessage.error('加载统计失败')
  } finally {
    loading.value = false
  }
})

function initCharts(data: AdminStats) {
  const dailyEl = document.getElementById('chart-daily')
  const boardEl = document.getElementById('chart-board')
  const userEl = document.getElementById('chart-user')
  if (!dailyEl || !boardEl || !userEl) {
    console.warn('[Admin] chart containers not found, retrying...')
    setTimeout(() => initCharts(data), 100)
    return
  }

  const daily = echarts.init(dailyEl)
  daily.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, bottom: 24, top: 8 },
    xAxis: { type: 'category', data: data.dailyPosts.map(d => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line', data: data.dailyPosts.map(d => d.count),
      smooth: true, areaStyle: { opacity: 0.15 },
      lineStyle: { color: '#b52b2b' },
      itemStyle: { color: '#b52b2b' },
    }],
  })

  const board = echarts.init(boardEl)
  board.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['30%', '60%'],
      data: data.postsPerBoard.map(b => ({ name: b.name, value: b.count })),
      label: { show: true, formatter: '{b}: {c}' },
      itemStyle: {
        color: ['#b52b2b', '#c94f4f', '#df7a7a', '#eba6a6', '#f5c6c6'],
      },
    }],
  })

  const userChart = echarts.init(userEl)
  userChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 16, bottom: 24, top: 8 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: data.topUsers.map(u => u.username).reverse() },
    series: [{
      type: 'bar', data: data.topUsers.map(u => u.count).reverse(),
      itemStyle: { color: '#b52b2b' },
      barMaxWidth: 32,
    }],
  })

  window.addEventListener('resize', () => {
    daily.resize()
    board.resize()
    userChart.resize()
  })
}
</script>

<template>
  <div class="admin-page">
    <div class="inner">
      <h2 class="mf-page-title">管理后台</h2>
      <p class="mf-page-sub">系统数据概览</p>

      <el-skeleton v-if="loading" :rows="8" animated />
      <el-empty v-else-if="!stats" description="暂无数据" />

      <template v-else>
        <div class="stat-cards">
          <div class="sc mf-card mf-fade-in">
            <span class="sc-n">{{ stats.userCount }}</span>
            <span class="sc-l">注册用户</span>
          </div>
          <div class="sc mf-card mf-fade-in mf-fade-in-d1">
            <span class="sc-n">{{ stats.postCount }}</span>
            <span class="sc-l">总帖子</span>
          </div>
          <div class="sc mf-card mf-fade-in mf-fade-in-d2">
            <span class="sc-n">{{ stats.replyCount }}</span>
            <span class="sc-l">总回复</span>
          </div>
          <div class="sc mf-card mf-fade-in mf-fade-in-d3">
            <span class="sc-n">{{ stats.boardCount }}</span>
            <span class="sc-l">板块数</span>
          </div>
        </div>

        <div class="chart-row">
          <div class="chart-box mf-card">
            <h3>近 7 日发帖趋势</h3>
            <div id="chart-daily" class="chart" />
          </div>
          <div class="chart-box mf-card">
            <h3>帖子分布（按板块）</h3>
            <div id="chart-board" class="chart" />
          </div>
        </div>

        <div class="chart-row">
          <div class="chart-box full mf-card">
            <h3>发帖最多用户</h3>
            <div id="chart-user" class="chart chart-sm" />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.admin-page { padding: 24px 16px 48px; }
.inner { max-width: 1000px; margin: 0 auto; }

.stat-cards {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px; margin-bottom: 28px;
}
.sc {
  text-align: center;
  padding: 24px 16px;
  cursor: default;
}
.sc-n {
  display: block;
  font-size: 2.2rem;
  font-weight: 800;
  color: var(--mf-primary);
  line-height: 1.1;
}
.sc-l {
  font-size: 13px;
  color: var(--mf-muted);
  margin-top: 6px;
  display: block;
}

.chart-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.chart-box {
  flex: 1;
  padding: 18px 16px 12px;
}
.chart-box h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: var(--mf-text);
}
.chart { height: 280px; }
.chart-sm { height: 200px; }
.chart-box.full { flex: none; width: 100%; }
</style>
