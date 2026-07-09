<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserByUserNo, type UserRow } from '@/api/user'

const userNoInput = ref<number | null>(null)
const loading = ref(false)
const user = ref<UserRow | null>(null)

async function query() {
  const no = userNoInput.value
  if (no == null || no < 10000000) {
    ElMessage.warning('请输入有效的 Mo 号（8～12 位数字）')
    return
  }
  loading.value = true
  user.value = null
  try {
    const { data } = await getUserByUserNo(no)
    user.value = data ?? null
  } catch {
    user.value = null
    ElMessage.info('该 Mo 号不存在')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="uq-page">
    <div class="inner">
      <h2 class="mf-page-title">按 Mo 号寻人</h2>
      <p class="mf-page-sub">Mo 号类似 QQ 号，注册后自动生成；可凭 Mo 号查找用户公开资料（不含密码）</p>

      <el-card class="panel" shadow="never">
        <el-form inline @submit.prevent="query">
          <el-form-item label="Mo 号">
            <el-input-number
              v-model="userNoInput"
              :min="10000000"
              :max="999999999999"
              :step="1"
              controls-position="right"
              placeholder="例如 1000000001"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" native-type="submit">查询</el-button>
          </el-form-item>
        </el-form>

        <el-descriptions v-if="user" :column="1" border class="result">
          <el-descriptions-item label="Mo 号">{{ user.userNo ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="内部 ID">{{ user.id }}</el-descriptions-item>
          <el-descriptions-item v-if="user.createTime" label="注册时间">{{ user.createTime }}</el-descriptions-item>
          <el-descriptions-item v-if="user.updateTime" label="更新时间">{{ user.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.uq-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 640px;
  margin: 0 auto;
}

.panel {
  border-radius: 14px;
  border: 1px solid var(--mf-border);
  background: var(--mf-paper);
}

.result {
  margin-top: 8px;
}
</style>
