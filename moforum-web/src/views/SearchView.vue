<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { search, type SearchResult } from '@/api/search'
import type { PostRow } from '@/api/post'
import type { BoardRow } from '@/api/board'
import type { UserRow } from '@/api/user'

const route = useRoute()
const router = useRouter()

const q = ref('')
const loading = ref(false)
const result = ref<SearchResult>({})
const activeTab = ref('posts')

onMounted(() => {
  const queryQ = route.query.q
  if (typeof queryQ === 'string' && queryQ.trim()) {
    q.value = queryQ.trim()
    doSearch()
  }
})

function doSearch() {
  if (!q.value.trim()) return
  loading.value = true
  search(q.value.trim())
    .then(({ data }) => {
      result.value = data ?? {}
    })
    .catch(() => {
      result.value = {}
    })
    .finally(() => {
      loading.value = false
    })
}

function goPost(id: number) {
  router.push({ name: 'post-detail', params: { id: String(id) } })
}

function goBoard(id: number) {
  router.push({ name: 'board', params: { boardId: String(id) } })
}

function goUser(id: number) {
  router.push({ name: 'user-profile', params: { userId: String(id) } })
}

function truncate(text: string, max = 150) {
  return text.length > max ? text.slice(0, max) + '…' : text
}
</script>

<template>
  <div class="search-page">
    <div class="inner">
      <div class="search-bar">
        <el-input
          v-model="q"
          placeholder="搜索帖子、贴吧、用户…"
          size="large"
          clearable
          :prefix-icon="Search"
          @keyup.enter="doSearch"
        />
      </div>

      <div v-loading="loading" class="results">
        <el-empty v-if="!loading && !result.posts?.length && !result.boards?.length && !result.users?.length" description="没有找到相关内容" />

        <el-tabs v-else v-model="activeTab">
          <el-tab-pane label="帖子" name="posts">
            <div v-if="!result.posts?.length" class="empty-tab">无匹配帖子</div>
            <div v-else class="post-list">
              <div v-for="p in result.posts" :key="p.id" class="post-item" @click="goPost(p.id)">
                <div class="post-title">{{ p.title }}</div>
                <div class="post-snippet">{{ truncate(p.content) }}</div>
                <div class="post-meta">
                  <el-tag v-if="p.boardName" size="small" type="danger" effect="light">{{ p.boardName }}</el-tag>
                  <span class="author">
                    <el-avatar v-if="!p.avatarUrl" :size="20" class="mini-av">{{ (p.authorName || '匿').slice(0, 1) }}</el-avatar>
                    <el-avatar v-else :size="20" :src="p.avatarUrl" class="mini-av-img" />
                    {{ p.authorName || '匿名' }}
                  </span>
                  <span v-if="p.createTime" class="time">{{ p.createTime }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="贴吧" name="boards">
            <div v-if="!result.boards?.length" class="empty-tab">无匹配贴吧</div>
            <el-row v-else :gutter="16">
              <el-col v-for="b in result.boards" :key="b.id" :xs="24" :sm="12" :md="8">
                <div class="board-card" @click="goBoard(b.id)">
                  <div class="board-top">
                    <span class="board-emoji">{{ b.avatar || '📌' }}</span>
                    <div>
                      <div class="board-name">{{ b.name }}</div>
                      <div class="board-count">{{ b.postCount ?? 0 }} 篇帖子</div>
                    </div>
                  </div>
                  <p class="board-desc">{{ b.description || '暂无简介' }}</p>
                </div>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="用户" name="users">
            <div v-if="!result.users?.length" class="empty-tab">无匹配用户</div>
            <div v-else class="user-list">
              <div v-for="u in result.users" :key="u.id" class="user-item" @click="goUser(u.id)">
                <el-avatar v-if="!u.avatarUrl" :size="40" class="user-avatar">{{ (u.username || '?').slice(0, 1) }}</el-avatar>
                <el-avatar v-else :size="40" :src="u.avatarUrl" class="user-avatar-img" />
                <div class="user-info">
                  <div class="user-name">{{ u.username }}</div>
                  <div class="user-no" v-if="u.userNo">Mo 号 {{ u.userNo }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 920px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 20px;
}

.results {
  min-height: 200px;
}

.empty-tab {
  text-align: center;
  padding: 40px 0;
  color: var(--mf-muted);
  font-size: 14px;
}

.post-list {
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.post-item {
  padding: 14px 18px;
  border-bottom: 1px solid var(--mf-border);
  cursor: pointer;
  transition: background 0.15s ease;
}

.post-item:last-child {
  border-bottom: none;
}

.post-item:hover {
  background: var(--mf-primary-soft);
}

.post-title {
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 6px;
}

.post-snippet {
  font-size: 13px;
  color: var(--mf-muted);
  line-height: 1.45;
  margin-bottom: 8px;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--mf-muted);
}

.author {
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

.board-card {
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  padding: 16px;
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

.board-count {
  font-size: 12px;
  color: var(--mf-muted);
  margin-top: 2px;
}

.board-desc {
  margin: 0;
  font-size: 13px;
  color: var(--mf-muted);
  line-height: 1.45;
}

.user-list {
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--mf-border);
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-item:last-child {
  border-bottom: none;
}

.user-item:hover {
  background: var(--mf-primary-soft);
}

.user-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 700;
  font-size: 15px;
}

.user-no {
  font-size: 12px;
  color: var(--mf-muted);
  margin-top: 2px;
}
</style>
