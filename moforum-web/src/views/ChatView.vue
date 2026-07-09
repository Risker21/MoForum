<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConversations, getMessages, sendMessage, markRead, type Conversation, type MessageRow } from '@/api/message'
import { uploadImage, confirmImage } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const friendId = computed(() => {
  const id = route.params.friendId
  return id ? Number(id) : null
})

const conversations = ref<Conversation[]>([])
const messages = ref<MessageRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const inputContent = ref('')
const loadingConv = ref(true)
const loadingMsg = ref(false)
const sending = ref(false)
const messagesEnd = ref<HTMLElement | null>(null)
const messageList = ref<HTMLElement | null>(null)
const imgUploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const emojiOpen = ref(false)

const EMOJIS = ['😀', '😂', '😍', '🥰', '😎', '🤔', '👍', '👏', '🎉', '❤️', '🔥', '💯', '😅', '🙈', '✨', '😭', '😤', '💪']

const currentConv = computed(() => {
  if (!friendId.value) return null
  return conversations.value.find((c) => c.userId === friendId.value) ?? null
})

const currentFriendName = computed(() => {
  const conv = currentConv.value
  if (conv) return conv.username
  const paramName = route.query.name
  return typeof paramName === 'string' ? paramName : '用户'
})

const currentFriendAvatar = computed(() => {
  return currentConv.value?.avatarUrl ?? ''
})

const ws = useWebSocket()

onMounted(async () => {
  await loadConversations()
  if (friendId.value) {
    await loadMessages()
    await doMarkRead()
  }
  connectWs()
})

watch(friendId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    page.value = 1
    messages.value = []
    await loadMessages()
    await doMarkRead()
  }
})

function isImageUrl(url: string): boolean {
  return /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp|bmp)(\?.*)?$/i.test(url.trim())
}

function renderMsg(content: string): string {
  return content
}

async function loadConversations() {
  loadingConv.value = true
  try {
    const { data } = await getConversations()
    conversations.value = data?.list ?? []
  } catch {
    conversations.value = []
  } finally {
    loadingConv.value = false
  }
}

async function loadMessages() {
  if (!friendId.value) return
  loadingMsg.value = true
  try {
    const { data } = await getMessages(friendId.value, page.value, pageSize.value)
    messages.value = (data?.list ?? []).reverse()
    total.value = data?.total ?? 0
    await nextTick()
    scrollToBottom()
  } catch {
    messages.value = []
    total.value = 0
  } finally {
    loadingMsg.value = false
  }
}

async function loadMore() {
  if (!friendId.value || messages.value.length >= total.value) return
  page.value++
  try {
    const { data } = await getMessages(friendId.value, page.value, pageSize.value)
    const older = (data?.list ?? []).reverse()
    messages.value = [...older, ...messages.value]
  } catch {
    //
  }
}

async function doMarkRead() {
  if (!friendId.value) return
  try {
    await markRead(friendId.value)
    const conv = conversations.value.find((c) => c.userId === friendId.value)
    if (conv) conv.unread = 0
    notificationStore.refresh()
  } catch {
    //
  }
}

async function handleSend() {
  const content = inputContent.value.trim()
  if (!content || !friendId.value) return
  sending.value = true
  try {
    const { data } = await sendMessage(friendId.value, content)
    if (data.success) {
      const msg: MessageRow = {
        id: data.id || Date.now(),
        fromId: userStore.userId!,
        toId: friendId.value,
        content,
        read: 1,
        createTime: new Date().toISOString(),
      }
      messages.value.push(msg)
      inputContent.value = ''
      updateConversation(friendId.value, content)
      await nextTick()
      scrollToBottom()
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发送失败')
  } finally {
    sending.value = false
  }
}

function handleInputKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function updateConversation(uid: number, lastMsg: string) {
  const conv = conversations.value.find((c) => c.userId === uid)
  if (conv) {
    conv.lastContent = lastMsg
    conv.lastTime = new Date().toISOString()
  }
}

function connectWs() {
  const token = localStorage.getItem('moforum_token')
  if (!token) return
  ws.connect(token)
  ws.onMessage((stompMsg) => {
    try {
      const msg: MessageRow = JSON.parse(stompMsg.body)
      if (friendId.value === msg.fromId) {
        messages.value.push(msg)
        nextTick(() => scrollToBottom())
        doMarkRead()
      }
      updateConversation(msg.fromId, msg.content)
      notificationStore.refresh()
    } catch {
      //
    }
  })
}

function scrollToBottom() {
  messagesEnd.value?.scrollIntoView({ behavior: 'smooth' })
}

function selectFriend(uid: number) {
  router.push(`/chat/${uid}`)
}

function onScroll() {
  const el = messageList.value
  if (!el || page.value * pageSize.value >= total.value) return
  if (el.scrollTop < 80) {
    loadMore()
  }
}

function formatTime(t?: string) {
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
}

function triggerImgUpload() {
  fileInput.value?.click()
}

async function onImgSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  if (file.size > 3 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 3MB')
    return
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持图片格式')
    return
  }
  imgUploading.value = true
  try {
    const { data: uploadRes } = await uploadImage(file)
    if (uploadRes.success && uploadRes.url) {
      confirmImage(uploadRes.url)
      inputContent.value += (inputContent.value ? '\n' : '') + uploadRes.url
    } else {
      ElMessage.error(uploadRes.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    imgUploading.value = false
  }
}

function insertEmoji(e: string) {
  inputContent.value += e
  emojiOpen.value = false
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-inner">
      <aside class="sidebar">
        <div class="sidebar-header">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="sidebar-icon">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span>消息</span>
        </div>
        <div v-loading="loadingConv" class="sidebar-list">
          <div v-if="!loadingConv && !conversations.length" class="empty-sidebar">
            <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <p>暂无对话</p>
          </div>
          <div
            v-for="conv in conversations"
            :key="conv.userId"
            class="conv-item"
            :class="{ active: conv.userId === friendId }"
            @click="selectFriend(conv.userId)"
          >
            <div class="conv-avatar-wrap">
              <el-badge :value="conv.unread" :hidden="!conv.unread" :max="99" class="conv-badge">
                <el-avatar v-if="!conv.avatarUrl" :size="42" class="conv-avatar">{{ (conv.username || '?').slice(0, 1) }}</el-avatar>
                <el-avatar v-else :size="42" :src="conv.avatarUrl" class="conv-avatar-img" />
              </el-badge>
            </div>
            <div class="conv-info">
              <div class="conv-name">{{ conv.username }}</div>
              <div class="conv-preview">{{ conv.lastContent }}</div>
            </div>
            <div class="conv-right">
              <div class="conv-time" v-if="conv.lastTime">{{ formatTime(conv.lastTime) }}</div>
            </div>
          </div>
        </div>
      </aside>

      <main class="chat-main">
        <template v-if="!friendId">
          <div class="empty-chat">
            <div class="empty-chat-inner">
              <svg viewBox="0 0 24 24" width="52" height="52" fill="none" stroke="var(--mf-primary)" stroke-width="1.2" class="empty-chat-icon">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <h3>选择一个好友开始聊天</h3>
              <p>从左侧列表选择对话，或前往好友页面发起新消息</p>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="chat-header">
            <button class="back-btn" @click="router.push('/friends')">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7" /></svg>
            </button>
            <div class="chat-header-avatar">
              <el-avatar v-if="!currentFriendAvatar" :size="34" class="h-avatar">{{ currentFriendName.slice(0, 1) }}</el-avatar>
              <el-avatar v-else :size="34" :src="currentFriendAvatar" class="h-avatar-img" />
            </div>
            <div class="chat-header-info">
              <div class="chat-header-name">{{ currentFriendName }}</div>
            </div>
          </div>

          <div ref="messageList" class="message-area" @scroll="onScroll">
            <div v-if="page * pageSize < total" class="load-more" @click="loadMore">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align:middle;margin-right:4px"><path d="M6 9l6 6 6-6" /></svg>
              加载更多
            </div>

            <div v-loading="loadingMsg" class="messages-inner">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="msg-row"
                :class="{ sent: msg.fromId === userStore.userId, received: msg.fromId !== userStore.userId }"
              >
                <div v-if="msg.fromId !== userStore.userId" class="msg-avatar-col">
                  <el-avatar v-if="!currentFriendAvatar" :size="30" class="msg-avatar-mini">{{ currentFriendName.slice(0, 1) }}</el-avatar>
                  <el-avatar v-else :size="30" :src="currentFriendAvatar" class="msg-avatar-img-mini" />
                </div>
                <div class="msg-content-col">
                  <div class="msg-bubble">
                    <template v-if="isImageUrl(msg.content)">
                      <img :src="msg.content" class="msg-img" alt="图片" @load="scrollToBottom" />
                    </template>
                    <template v-else>
                      <span class="msg-text">{{ msg.content }}</span>
                    </template>
                  </div>
                  <div class="msg-meta">
                    <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                    <span v-if="msg.fromId === userStore.userId && msg.read" class="msg-read-tick">✓✓</span>
                  </div>
                </div>
              </div>
              <div ref="messagesEnd" class="msgs-end" />
            </div>
          </div>

          <div class="input-area">
            <div class="input-toolbar">
              <button class="tool-btn" @click="triggerImgUpload" title="发送图片" :class="{ loading: imgUploading }">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="M21 15l-5-5L5 21" />
                </svg>
              </button>
              <input ref="fileInput" type="file" accept="image/*" hidden @change="onImgSelected" />
              <div class="emoji-wrapper">
                <button class="tool-btn" @click="emojiOpen = !emojiOpen" title="表情">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M8 14s1.5 2 4 2 4-2 4-2" />
                    <line x1="9" y1="9" x2="9.01" y2="9" />
                    <line x1="15" y1="9" x2="15.01" y2="9" />
                  </svg>
                </button>
                <div v-if="emojiOpen" class="emoji-panel" @mouseleave="emojiOpen = false">
                  <span v-for="e in EMOJIS" :key="e" class="emoji-cell" @click="insertEmoji(e)">{{ e }}</span>
                </div>
              </div>
            </div>
            <div class="input-row">
              <textarea
                v-model="inputContent"
                class="msg-input"
                placeholder="输入消息…Enter 发送"
                rows="1"
                @keydown="handleInputKeydown"
                @input="$el => { $el.target.style.height = 'auto'; $el.target.style.height = Math.min($el.target.scrollHeight, 120) + 'px' }"
              />
              <button class="send-btn" :disabled="!inputContent.trim() || sending" @click="handleSend">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </button>
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  height: calc(100vh - var(--mf-header-h) - 48px);
  padding: 16px 20px;
}

.chat-inner {
  display: flex;
  height: 100%;
  max-width: 1120px;
  margin: 0 auto;
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

/* ── Sidebar ── */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--mf-border);
  display: flex;
  flex-direction: column;
  background: #faf8f5;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 16px 14px;
  font-weight: 800;
  font-size: 15px;
  color: var(--mf-text);
  letter-spacing: 0.02em;
}

.sidebar-icon {
  color: var(--mf-primary);
  flex-shrink: 0;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.empty-sidebar {
  text-align: center;
  padding: 60px 16px 40px;
  color: var(--mf-muted);
  font-size: 13px;
}

.empty-icon {
  color: #d6d0c8;
  margin-bottom: 12px;
}

.empty-sidebar p {
  margin: 0;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 12px;
  transition: background 0.15s ease;
  margin-bottom: 2px;
}

.conv-item:hover {
  background: #edeae5;
}

.conv-item.active {
  background: var(--mf-primary-soft);
}

.conv-avatar-wrap {
  flex-shrink: 0;
}

.conv-badge :deep(.el-badge__content) {
  border: 2px solid #faf8f5;
  font-size: 10px;
  height: 18px;
  line-height: 18px;
}

.conv-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-weight: 700;
}

.conv-avatar-img {
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-weight: 700;
  font-size: 13.5px;
  color: var(--mf-text);
}

.conv-preview {
  font-size: 12px;
  color: var(--mf-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.conv-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.conv-time {
  font-size: 10.5px;
  color: #bbb;
  white-space: nowrap;
}

/* ── Chat Main ── */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-chat-inner {
  text-align: center;
  color: var(--mf-muted);
}

.empty-chat-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-chat-inner h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: var(--mf-text);
}

.empty-chat-inner p {
  margin: 0;
  font-size: 13px;
}

/* ── Chat Header ── */
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--mf-border);
  background: var(--mf-paper);
}

.back-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--mf-muted);
  padding: 4px;
  border-radius: 8px;
  transition: background 0.15s;
}

.back-btn:hover {
  background: #f0f0f0;
}

@media (max-width: 640px) {
  .back-btn { display: flex; }
}

.h-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-weight: 700;
}

.chat-header-info {
  flex: 1;
}

.chat-header-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--mf-text);
}

/* ── Message Area ── */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #fcfaf8;
}

.load-more {
  text-align: center;
  font-size: 12.5px;
  color: var(--mf-primary);
  cursor: pointer;
  padding: 10px 8px 14px;
  opacity: 0.7;
  transition: opacity 0.15s;
}

.load-more:hover {
  opacity: 1;
}

.messages-inner {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msgs-end {
  height: 1px;
}

.msg-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  animation: msg-in 0.2s ease both;
}

@keyframes msg-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-row.sent {
  justify-content: flex-end;
}

.msg-avatar-col {
  flex-shrink: 0;
}

.msg-avatar-mini {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-weight: 700;
  font-size: 12px;
}

.msg-avatar-img-mini {
  flex-shrink: 0;
}

.msg-content-col {
  max-width: 65%;
  display: flex;
  flex-direction: column;
}

.msg-row.sent .msg-content-col {
  align-items: flex-end;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  position: relative;
}

.msg-text {
  white-space: pre-wrap;
}

.msg-img {
  max-width: 240px;
  max-height: 280px;
  border-radius: 10px;
  display: block;
  cursor: pointer;
}

.msg-row.sent .msg-bubble {
  background: var(--mf-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 1px 3px rgba(181,43,43,0.15);
}

.msg-row.received .msg-bubble {
  background: #fff;
  color: var(--mf-text);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border: 1px solid #ece8e3;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  padding: 0 4px;
}

.msg-row.sent .msg-meta {
  justify-content: flex-end;
}

.msg-time {
  font-size: 10.5px;
  color: #bbb;
}

.msg-read-tick {
  font-size: 10px;
  color: var(--mf-primary);
  opacity: 0.7;
}

/* ── Input Area ── */
.input-area {
  border-top: 1px solid var(--mf-border);
  padding: 10px 16px 14px;
  background: var(--mf-paper);
}

.input-toolbar {
  display: flex;
  gap: 2px;
  margin-bottom: 6px;
  align-items: center;
}

.tool-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  color: #999;
  transition: background 0.15s, color 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tool-btn:hover {
  background: #f0eeeb;
  color: var(--mf-primary);
}

.tool-btn.loading {
  opacity: 0.5;
  pointer-events: none;
}

.emoji-wrapper {
  position: relative;
}

.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 6px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 3px;
  padding: 8px;
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  z-index: 50;
}

.emoji-cell {
  font-size: 22px;
  text-align: center;
  cursor: pointer;
  padding: 4px 2px;
  border-radius: 6px;
  transition: background 0.1s;
}

.emoji-cell:hover {
  background: var(--mf-primary-soft);
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.msg-input {
  flex: 1;
  border: 1px solid #e0dbd5;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  line-height: 1.45;
  max-height: 120px;
  background: #faf8f5;
  transition: border-color 0.15s, background 0.15s;
}

.msg-input:focus {
  border-color: var(--mf-primary);
  background: #fff;
}

.msg-input::placeholder {
  color: #c5bfb8;
}

.send-btn {
  background: var(--mf-primary);
  border: none;
  color: #fff;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, transform 0.1s;
  flex-shrink: 0;
}

.send-btn:hover {
  background: var(--mf-primary-dark);
  transform: scale(1.04);
}

.send-btn:disabled {
  background: #d6d0c8;
  cursor: not-allowed;
  transform: none;
}

/* ── Scrollbar ── */
.sidebar-list::-webkit-scrollbar,
.message-area::-webkit-scrollbar {
  width: 4px;
}

.sidebar-list::-webkit-scrollbar-track,
.message-area::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-list::-webkit-scrollbar-thumb,
.message-area::-webkit-scrollbar-thumb {
  background: #d6d0c8;
  border-radius: 4px;
}

.sidebar-list::-webkit-scrollbar-thumb:hover,
.message-area::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}

@media (max-width: 720px) {
  .chat-page {
    padding: 0;
  }
  .chat-inner {
    border-radius: 0;
    border: none;
  }
  .sidebar {
    width: 240px;
  }
}
</style>
