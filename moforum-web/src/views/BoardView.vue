<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBoard, type BoardRow } from '@/api/board'
import { listPostsByBoard, type PostRow } from '@/api/post'

const route = useRoute()
const router = useRouter()

const boardId = computed(() => Number(route.params.boardId))
const board = ref<BoardRow | null>(null)
const list = ref<PostRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(15)
const loading = ref(true)

async function loadBoard() {
  if (!Number.isFinite(boardId.value) || boardId.value < 1) {
    ElMessage.error('无效的吧 ID')
    return
  }
  try {
    const { data } = await getBoard(boardId.value)
    board.value = data ?? null
    if (!board.value?.id) {
      ElMessage.warning('贴吧不存在')
    }
  } catch {
    board.value = null
    ElMessage.error('加载贴吧失败')
  }
}

async function loadPosts() {
  if (!Number.isFinite(boardId.value) || boardId.value < 1) return
  loading.value = true
  try {
    const { data } = await listPostsByBoard(boardId.value, page.value, pageSize.value)
    list.value = data?.list ?? []
    total.value = data?.total ?? 0
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载帖子失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadBoard()
  await loadPosts()
})

watch(
  () => route.params.boardId,
  async () => {
    page.value = 1
    await loadBoard()
    await loadPosts()
  },
)

watch([page, pageSize], loadPosts)

function goPost(id: number) {
  router.push({ name: 'post-detail', params: { id: String(id) } })
}

function goUser(id: number) {
  router.push({ name: 'user-profile', params: { userId: String(id) } })
}

function newPost() {
  router.push({ name: 'post-new', params: { boardId: String(boardId.value) } })
}
</script>

<template>
  <div class="board-page">
    <div class="inner">
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ name: 'home' }">广场</el-breadcrumb-item>
        <el-breadcrumb-item>{{ board?.name || '贴吧' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <header class="head mf-card">
        <div class="head-left">
          <span class="emoji">{{ board?.avatar || '📌' }}</span>
          <div>
            <h1 class="title">{{ board?.name || '加载中…' }}</h1>
            <p class="sub">{{ board?.description || ' ' }}</p>
          </div>
        </div>
        <el-button type="primary" size="large" round @click="newPost">发新帖</el-button>
      </header>

      <el-skeleton v-if="loading && !list.length" :rows="6" animated />

      <el-empty v-else-if="!list.length" description="本吧还没有帖子，来发第一条吧">
        <el-button type="primary" round @click="newPost">发帖</el-button>
      </el-empty>

      <div v-else class="post-list mf-card">
        <div
          v-for="(p, i) in list"
          :key="p.id"
          class="post-row mf-fade-in"
          :class="'mf-fade-in-d' + Math.min(i + 1, 6)"
          @click="goPost(p.id)"
        >
          <div class="post-main">
            <span class="post-title">{{ p.title }}</span>
            <span class="post-snippet">{{ p.content }}</span>
          </div>
          <div class="post-meta">
            <span class="link author" @click.stop="goUser(p.userId)">
              <el-avatar v-if="!p.avatarUrl" :size="20" class="mini-av">{{ (p.authorName || '匿').slice(0, 1) }}</el-avatar>
              <el-avatar v-else :size="20" :src="p.avatarUrl" class="mini-av-img" />
              {{ p.authorName || '匿名' }}
            </span>
            <span>{{ p.replyCount ?? 0 }} 回复</span>
            <span>{{ p.viewCount ?? 0 }} 浏览</span>
            <span v-if="p.createTime" class="time">{{ p.createTime }}</span>
          </div>
        </div>
      </div>

      <div v-if="total > 0" class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 15, 30]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 920px;
  margin: 0 auto;
}

.crumb {
  margin-bottom: 16px;
  font-size: 13px;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  margin-bottom: 20px;
}

.head-left {
  display: flex;
  gap: 14px;
  align-items: center;
}

.emoji {
  font-size: 2.5rem;
}

.title {
  margin: 0 0 4px;
  font-size: 1.35rem;
  font-weight: 800;
}

.sub {
  margin: 0;
  font-size: 13px;
  color: var(--mf-muted);
  max-width: 520px;
  line-height: 1.5;
}

.post-list {
  overflow: hidden;
}

.post-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--mf-border);
  cursor: pointer;
  transition: background 0.15s ease;
}

.post-row:last-child {
  border-bottom: none;
}

.post-row:hover {
  background: var(--mf-primary-soft);
}

.post-main {
  flex: 1;
  min-width: 200px;
}

.post-title {
  display: block;
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 4px;
}

.post-snippet {
  font-size: 13px;
  color: var(--mf-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--mf-muted);
  align-items: flex-start;
}

.post-meta .time {
  color: #999;
}

.link { cursor: pointer; color: #5d4037; font-weight: 600; }
.link:hover { color: var(--mf-primary); }
.author { display: inline-flex; align-items: center; gap: 6px; }
.mini-av { background: linear-gradient(135deg, #b52b2b, #c0392b); color: #fff; font-size: 11px; flex-shrink: 0; }
.mini-av-img { flex-shrink: 0; }

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
