<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listBoards, type BoardRow } from '@/api/board'
import { latestPosts, type PostRow } from '@/api/post'

const router = useRouter()
const boards = ref<BoardRow[]>([])
const hot = ref<PostRow[]>([])
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  const [b, h] = await Promise.all([
    listBoards().catch(() => ({ data: [] as BoardRow[] })),
    latestPosts(10).catch(() => ({ data: [] as PostRow[] })),
  ])
  boards.value = b.data ?? []
  hot.value = h.data ?? []
  loading.value = false
})

function goBoard(id: number) {
  router.push({ name: 'board', params: { boardId: String(id) } })
}

function goPost(id: number) {
  router.push({ name: 'post-detail', params: { id: String(id) } })
}

function goUser(id: number) {
  router.push({ name: 'user-profile', params: { userId: String(id) } })
}

function scrollBoards() {
  document.getElementById('boards-section')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>方寸之间，自有天地</h1>
          <p>选一个你感兴趣的吧，和同好一起聊。全站热帖实时刷新。</p>
          <el-button type="primary" size="large" round @click="scrollBoards">逛逛贴吧</el-button>
        </div>
        <div class="hero-stats" aria-hidden="true">
          <div class="stat-card">
            <span class="n">{{ boards.length }}</span>
            <span class="l">贴吧</span>
          </div>
          <div class="stat-card">
            <span class="n">{{ hot.length }}</span>
            <span class="l">热帖</span>
          </div>
        </div>
      </div>
    </section>

    <div class="content">
      <div id="boards-section" class="section-head">
        <h2 class="section-title">贴吧广场</h2>
        <p class="section-desc">每个吧都有独立帖子列表，点击卡片进入</p>
      </div>

      <el-skeleton v-if="loading" :rows="3" animated />

      <el-row v-else :gutter="16" class="board-row">
        <el-col v-for="(b, i) in boards" :key="b.id" :xs="24" :sm="12" :md="8" :lg="6">
          <div
            class="board-card mf-fade-in"
            :class="'mf-fade-in-d' + Math.min(i + 1, 6)"
            role="button"
            tabindex="0"
            @click="goBoard(b.id)"
            @keyup.enter="goBoard(b.id)"
          >
            <div class="board-top">
              <span class="board-emoji">{{ b.avatar || '📌' }}</span>
              <div class="board-info">
                <div class="board-name">{{ b.name }}</div>
                <div class="board-meta">{{ b.postCount ?? 0 }} 篇帖子</div>
              </div>
            </div>
            <p class="board-desc">{{ b.description || '暂无简介' }}</p>
            <div class="board-go">进入贴吧 →</div>
          </div>
        </el-col>
      </el-row>

      <div class="section-head">
        <h2 class="section-title">全站新鲜帖</h2>
        <p class="section-desc">最新发布的帖子，点击标题查看详情</p>
      </div>

      <div v-if="!loading && !hot.length" class="empty-wrap mf-card">
        <el-empty description="还没有帖子，快去某个吧发首帖吧" />
      </div>

      <div v-else class="hot-list mf-card">
        <div
          v-for="(p, i) in hot"
          :key="p.id"
          class="hot-item mf-fade-in"
          :class="'mf-fade-in-d' + Math.min(i + 1, 6)"
          @click="goPost(p.id)"
        >
          <div class="hot-main">
            <span class="hot-title">{{ p.title }}</span>
            <span class="hot-snippet">{{ p.content }}</span>
          </div>
          <div class="hot-side">
            <el-tag v-if="p.boardName" size="small" type="danger" effect="plain" round>{{ p.boardName }}</el-tag>
            <span class="hot-author link" @click.stop="goUser(p.userId)">
              <el-avatar v-if="!p.avatarUrl" :size="20" class="mini-av">{{ (p.authorName || '已').slice(0, 1) }}</el-avatar>
              <el-avatar v-else :size="20" :src="p.avatarUrl" class="mini-av-img" />
              {{ p.authorName || '已注销' }}
            </span>
            <span class="hot-stat">{{ p.replyCount ?? 0 }} 回复</span>
            <span class="hot-stat">{{ p.viewCount ?? 0 }} 浏览</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: calc(100vh - 60px - 80px);
}

.hero {
  background: linear-gradient(135deg, #2d1b14 0%, #4a2820 40%, #6d3a2a 100%);
  color: #fff;
  padding: 40px 20px 44px;
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  top: -60%;
  right: -20%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(181,43,43,0.15) 0%, transparent 70%);
  pointer-events: none;
}

.hero-inner {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.hero-text h1 {
  margin: 0 0 8px;
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  font-weight: 800;
  letter-spacing: 0.06em;
  font-family: 'Noto Serif SC', 'PingFang SC', serif;
}

.hero-text p {
  margin: 0 0 20px;
  max-width: 480px;
  opacity: 0.85;
  line-height: 1.6;
  font-size: 0.95rem;
}

.hero-stats {
  display: flex;
  gap: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  padding: 14px 22px;
  min-width: 100px;
  text-align: center;
}

.stat-card .n {
  display: block;
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.2;
}

.stat-card .l {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}

.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.section-head {
  margin-bottom: 18px;
}

.section-title {
  margin: 0 0 2px;
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0.03em;
  color: var(--mf-text);
  font-family: 'Noto Serif SC', 'PingFang SC', serif;
}

.section-desc {
  margin: 0;
  font-size: 13px;
  color: var(--mf-muted);
}

.board-row {
  margin-bottom: 8px;
}

.board-card {
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  height: 100%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.board-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(181,43,43,0.10);
}

.board-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.board-emoji {
  font-size: 2rem;
  line-height: 1;
}

.board-name {
  font-weight: 800;
  font-size: 1.05rem;
}

.board-meta {
  font-size: 12px;
  color: var(--mf-muted);
  margin-top: 2px;
}

.board-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--mf-muted);
  line-height: 1.5;
  min-height: 2.6em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.board-go {
  font-size: 13px;
  font-weight: 700;
  color: var(--mf-primary);
  transition: letter-spacing 0.2s ease;
}

.board-card:hover .board-go {
  letter-spacing: 0.04em;
}

.hot-list {
  overflow: hidden;
}

.hot-item {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background 0.15s ease;
  border-bottom: 1px solid var(--mf-border);
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-item:hover {
  background: var(--mf-primary-soft);
}

.hot-main {
  flex: 1;
  min-width: 200px;
}

.hot-title {
  display: block;
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 4px;
  color: var(--mf-text);
}

.hot-snippet {
  font-size: 13px;
  color: var(--mf-muted);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-side {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--mf-muted);
}

.hot-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #5d4037;
}

.mini-av {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-size: 11px;
  flex-shrink: 0;
}

.mini-av-img {
  flex-shrink: 0;
}

.hot-stat {
  white-space: nowrap;
}

.link { cursor: pointer; }
.link:hover { color: var(--mf-primary); }

.empty-wrap {
  padding: 24px;
  border-style: dashed;
}
</style>
