<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFriendList, getReceivedRequests, getSentRequests, respondFriendRequest, type FriendUser, type FriendRequest } from '@/api/friend'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const friends = ref<FriendUser[]>([])
const received = ref<FriendRequest[]>([])
const sent = ref<FriendRequest[]>([])
const loading = ref(true)
const activeTab = ref('friends')

onMounted(async () => {
  loading.value = true
  try {
    const [f, r, s] = await Promise.all([
      getFriendList(),
      getReceivedRequests(),
      getSentRequests(),
    ])
    friends.value = f.data?.list ?? []
    received.value = r.data?.list ?? []
    sent.value = s.data?.list ?? []
  } catch {
    friends.value = []
    received.value = []
    sent.value = []
  } finally {
    loading.value = false
  }
})

async function handleRespond(requestId: number, accept: boolean) {
  const req = received.value.find((r) => r.id === requestId)
  if (!req) return
  try {
    const { data } = await respondFriendRequest(requestId, accept)
    if (data.success) {
      received.value = received.value.filter((r) => r.id !== requestId)
      if (accept) {
        friends.value.push({
          userId: req.fromId,
          username: req.username || '未知',
        })
      }
      notificationStore.refresh()
      ElMessage.success(data.message)
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

function goChat(userId: number) {
  router.push(`/chat/${userId}`).catch((e) => console.error('chat nav err', e))
}

function statusTag(status: string) {
  if (status === 'PENDING') return { type: 'warning' as const, text: '等待对方确认' }
  if (status === 'ACCEPTED') return { type: 'success' as const, text: '已接受' }
  return { type: 'danger' as const, text: '已拒绝' }
}

function goProfile(userId: number) {
  router.push({ name: 'user-profile', params: { userId: String(userId) } })
}
</script>

<template>
  <div class="friends-page">
    <div class="inner">
      <h2 class="mf-page-title">好友管理</h2>

      <el-skeleton v-if="loading" :rows="5" animated />

      <el-tabs v-else v-model="activeTab">
        <el-tab-pane label="我的好友" name="friends">
          <el-empty v-if="!friends.length" description="还没有好友，去用户主页发送好友请求吧" />
          <div v-else class="friend-list">
            <div v-for="f in friends" :key="f.userId" class="friend-item" @click="goProfile(f.userId)">
              <el-avatar v-if="!f.avatarUrl" :size="40" class="friend-avatar">{{ (f.username || '?').slice(0, 1) }}</el-avatar>
              <el-avatar v-else :size="40" :src="f.avatarUrl" class="friend-avatar-img" />
              <div class="friend-info">
                <div class="friend-name">{{ f.username }}</div>
                <div v-if="f.userNo" class="friend-no">Mo 号 {{ f.userNo }}</div>
              </div>
              <el-button type="primary" size="small" round @click.stop="goChat(f.userId)">发消息</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="收到的请求" name="received">
          <el-empty v-if="!received.length" description="没有收到好友请求" />
          <div v-else class="friend-list">
            <div v-for="r in received" :key="r.id" class="friend-item">
              <el-avatar :size="40" class="friend-avatar">{{ (r.username || '?').slice(0, 1) }}</el-avatar>
              <div class="friend-info">
                <div class="friend-name">{{ r.username || '未知用户' }}</div>
              </div>
              <div class="friend-actions">
                <el-button type="success" size="small" round @click="handleRespond(r.id, true)">接受</el-button>
                <el-button type="danger" size="small" round @click="handleRespond(r.id, false)">拒绝</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="已发送的请求" name="sent">
          <el-empty v-if="!sent.length" description="还没有发送过好友请求" />
          <div v-else class="friend-list">
            <div v-for="r in sent" :key="r.id" class="friend-item">
              <el-avatar :size="40" class="friend-avatar">{{ (r.username || '?').slice(0, 1) }}</el-avatar>
              <div class="friend-info">
                <div class="friend-name">{{ r.username || '未知用户' }}</div>
              </div>
              <el-tag :type="statusTag(r.status).type" effect="light" round>{{ statusTag(r.status).text }}</el-tag>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
.friends-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 720px;
  margin: 0 auto;
}

.friend-list {
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--mf-border);
  cursor: pointer;
  transition: background 0.15s ease;
}

.friend-item:last-child {
  border-bottom: none;
}

.friend-item:hover {
  background: var(--mf-primary-soft);
}

.friend-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  flex-shrink: 0;
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 700;
  font-size: 15px;
}

.friend-no {
  font-size: 12px;
  color: var(--mf-muted);
  margin-top: 2px;
}

.friend-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
</style>
