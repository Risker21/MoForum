<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { http } from '@/api/http'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const notificationStore = useNotificationStore()

const searchQ = ref('')

const menuPath = computed(() => {
  const p = route.path
  if (p.startsWith('/user/query')) return '/user/query'
  if (p === '/' || p.startsWith('/b/') || p.startsWith('/p/')) return '/'
  if (p.startsWith('/friends')) return '/friends'
  if (p.startsWith('/chat')) return '/chat'
  return p
})

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function logout() {
  try {
    await http.post('/user/logout')
  } catch { /* ignore */ }
  user.logout()
  router.push({ name: 'home' })
}

function doSearch() {
  const q = searchQ.value.trim()
  if (q) {
    router.push({ name: 'search', query: { q } })
  }
}

onMounted(() => {
  if (user.isLoggedIn) {
    notificationStore.refresh()
  }
})

watch(() => user.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    notificationStore.refresh()
  }
})
</script>

<template>
  <el-container class="mf-shell">
    <el-header class="mf-header">
      <div class="mf-header-inner">
        <RouterLink to="/" class="mf-logo">
          <span class="mf-logo-mark">Mo</span>
        </RouterLink>

        <el-menu
          :key="menuPath"
          :default-active="menuPath"
          mode="horizontal"
          class="mf-menu"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/">广场</el-menu-item>
          <el-menu-item index="/user/query">用户</el-menu-item>
          <el-menu-item v-if="user.isLoggedIn" index="/friends">
            <el-badge :value="notificationStore.friendRequestCount" :hidden="notificationStore.friendRequestCount === 0" :max="99">
              好友
            </el-badge>
          </el-menu-item>
          <el-menu-item v-if="user.isLoggedIn" index="/chat">
            <el-badge :value="notificationStore.unreadMessageCount" :hidden="notificationStore.unreadMessageCount === 0" :max="99">
              消息
            </el-badge>
          </el-menu-item>
        </el-menu>

        <div class="mf-search">
          <el-input
            v-model="searchQ"
            class="mf-search-input"
            placeholder="搜帖子、贴吧、用户…"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon style="cursor:pointer" @click="doSearch"><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="mf-actions">
          <template v-if="user.isLoggedIn">
            <el-dropdown trigger="click">
              <span class="mf-user-trigger">
                <el-avatar v-if="!user.avatarUrl" :size="30" class="mf-avatar">{{ user.username?.slice(0, 1) }}</el-avatar>
                <el-avatar v-else :size="30" :src="user.avatarUrl" class="mf-avatar-img" />
                <span class="mf-username">{{ user.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <div v-if="user.userNo" class="mf-dropdown-mo">Mo 号 {{ user.userNo }}</div>
                  <el-dropdown-item @click="router.push({ name: 'user-profile', params: { userId: String(user.userId) } })">我的主页</el-dropdown-item>
                  <el-dropdown-item v-if="user.username === 'admin'" @click="router.push({ name: 'admin' })">管理后台</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="goLogin">登录</el-button>
            <el-button round @click="router.push({ name: 'register' })">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="mf-main">
      <RouterView />
    </el-main>

    <el-footer class="mf-footer" height="auto">
      <p>MoForum · 方寸之间，自有天地</p>
    </el-footer>
  </el-container>
</template>

<style scoped>
.mf-shell {
  min-height: 100%;
  flex-direction: column;
  background: var(--mf-bg);
}

.mf-header {
  padding: 0;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--mf-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.mf-header-inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
}

.mf-logo {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  text-decoration: none;
}

.mf-logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-family: 'Playfair Display', Georgia, serif;
  font-weight: 800;
  font-size: 1.1rem;
  letter-spacing: 0.06em;
}

.mf-menu {
  flex-shrink: 0;
  border-bottom: none !important;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--mf-primary-soft);
  --el-menu-active-color: var(--mf-primary);
}

.mf-menu :deep(.el-menu-item) {
  font-weight: 600;
  font-size: 14px;
  border-radius: 8px;
  margin: 0 2px;
  transition: background 0.15s ease, color 0.15s ease;
}

.mf-search {
  flex: 1;
  max-width: 320px;
  min-width: 100px;
}

.mf-search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: var(--mf-bg);
  box-shadow: none;
  border: 1px solid transparent;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.mf-search-input :deep(.el-input__wrapper:hover) {
  border-color: var(--mf-border);
  background: var(--mf-paper);
}

.mf-search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--mf-primary);
  background: var(--mf-paper);
  box-shadow: 0 0 0 3px rgba(181,43,43,0.08);
}

.mf-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mf-user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px 4px 4px;
  border-radius: 999px;
  transition: background 0.15s ease;
}

.mf-user-trigger:hover {
  background: var(--mf-primary-soft);
}

.mf-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-size: 14px;
}

.mf-avatar-img {
  border: 2px solid var(--mf-border);
}

.mf-username {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.mf-main {
  padding: 0;
  overflow: visible;
}

.mf-dropdown-mo {
  padding: 6px 16px 10px;
  font-size: 12px;
  color: var(--mf-muted);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.mf-footer {
  padding: 24px 20px 32px;
  text-align: center;
  color: var(--mf-muted);
  font-size: 13px;
  background: transparent;
  letter-spacing: 0.04em;
}
</style>
