<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserById, updateProfile, type UserRow } from '@/api/user'
import { uploadImage, confirmImage } from '@/api/upload'
import { listPostsByUser, type PostRow } from '@/api/post'
import { toggleFollow, getFollowCounts, getFollowStatus } from '@/api/follow'
import { sendFriendRequest, getFriendStatus } from '@/api/friend'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const profile = ref<UserRow | null>(null)
const posts = ref<PostRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(true)
const counts = ref<{ followers: number; following: number }>({ followers: 0, following: 0 })
const following = ref(false)
const friendStatus = ref<'NONE' | 'PENDING' | 'ACCEPTED' | 'REJECTED'>('NONE')
const actionLoading = ref(false)

const isOwn = computed(() => userStore.isLoggedIn && userStore.userId === userId.value)

const editDialog = ref(false)
const editBio = ref('')
const editAvatar = ref('')
const editLoading = ref(false)

async function loadProfile() {
  if (!Number.isFinite(userId.value) || userId.value < 1) {
    ElMessage.error('无效的用户')
    return
  }
  loading.value = true
  try {
    const { data } = await getUserById(userId.value)
    if (!data?.id) {
      ElMessage.warning('用户不存在')
      profile.value = null
      return
    }
    profile.value = data
  } catch {
    profile.value = null
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  if (!profile.value) return
  try {
    const { data } = await getFollowCounts(userId.value)
    counts.value = data ?? { followers: 0, following: 0 }
  } catch {
    counts.value = { followers: 0, following: 0 }
  }
}

async function loadFollowStatus() {
  if (!userStore.isLoggedIn || !profile.value) return
  try {
    const { data } = await getFollowStatus(userId.value)
    following.value = (data as any)?.followed ?? false
  } catch {
    following.value = false
  }
}

async function loadFriendStatus() {
  if (!userStore.isLoggedIn || !profile.value) return
  try {
    const { data } = await getFriendStatus(userId.value)
    friendStatus.value = data?.status ?? 'NONE'
  } catch {
    friendStatus.value = 'NONE'
  }
}

async function loadPosts() {
  if (!profile.value) return
  try {
    const { data } = await listPostsByUser(userId.value, page.value, pageSize.value)
    posts.value = data?.list ?? []
    total.value = data?.total ?? 0
  } catch {
    posts.value = []
    total.value = 0
  }
}

onMounted(async () => {
  await loadProfile()
  await Promise.all([loadCounts(), loadPosts()])
  await Promise.all([loadFollowStatus(), loadFriendStatus()])
})

watch(() => route.params.userId, async () => {
  page.value = 1
  await loadProfile()
  await Promise.all([loadCounts(), loadPosts()])
  await Promise.all([loadFollowStatus(), loadFriendStatus()])
})

watch([page, pageSize], loadPosts)

async function handleToggleFollow() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  actionLoading.value = true
  try {
    const { data } = await toggleFollow(userId.value)
    if (data.success) {
      following.value = !following.value
      counts.value.followers += following.value ? 1 : -1
      ElMessage.success(data.message)
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleAddFriend() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  actionLoading.value = true
  try {
    const { data } = await sendFriendRequest(userId.value)
    if (data.success) {
      friendStatus.value = 'PENDING'
      ElMessage.success(data.message)
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    actionLoading.value = false
  }
}

function goPost(id: number) {
  router.push({ name: 'post-detail', params: { id: String(id) } })
}

function openEdit() {
  editBio.value = profile.value?.bio ?? ''
  editAvatar.value = ''
  editDialog.value = true
}

async function saveProfile() {
  if (editBio.value.length > 256) {
    ElMessage.warning('签名不能超过 256 字')
    return
  }
  editLoading.value = true
  try {
    const payload: { bio?: string; avatarUrl?: string } = { bio: editBio.value }
    if (editAvatar.value) {
      payload.avatarUrl = editAvatar.value
    }
    const { data } = await updateProfile(payload)
    if (data.success) {
      if (profile.value) {
        profile.value.bio = editBio.value
        if (editAvatar.value) {
          profile.value.avatarUrl = editAvatar.value
          userStore.updateAvatar(editAvatar.value)
        }
      }
      ElMessage.success(data.message)
      editDialog.value = false
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

async function onAvatarUpload(file: File) {
  if (file.size > 3 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 3MB')
    return
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持图片格式')
    return
  }
  const d = new FormData()
  d.append('file', file)
  try {
    const { data: uploadRes } = await uploadImage(file)
    if (uploadRes.success && uploadRes.url) {
      editAvatar.value = uploadRes.url
      confirmImage(uploadRes.url)
      ElMessage.success('头像已上传')
    } else {
      ElMessage.error(uploadRes.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="inner">
      <el-skeleton v-if="loading && !profile" :rows="5" animated />

      <template v-else-if="!profile">
        <el-empty description="用户不存在" />
      </template>

      <template v-else>
        <div class="profile-card">
          <div class="profile-left">
            <el-avatar v-if="!profile.avatarUrl" :size="72" class="profile-avatar">{{ (profile.username || '?').slice(0, 1) }}</el-avatar>
            <el-avatar v-else :size="72" :src="profile.avatarUrl" class="profile-avatar-img" />
            <div class="profile-info">
              <h1 class="profile-name">{{ profile.username }}</h1>
              <div v-if="profile.userNo" class="profile-no">Mo 号 {{ profile.userNo }}</div>
              <div v-if="profile.bio" class="profile-bio">{{ profile.bio }}</div>
            </div>
          </div>

          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-n">{{ total }}</span>
              <span class="stat-l">帖子</span>
            </div>
            <div class="stat-item">
              <span class="stat-n">{{ counts.followers }}</span>
              <span class="stat-l">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-n">{{ counts.following }}</span>
              <span class="stat-l">关注</span>
            </div>
          </div>

          <div v-if="isOwn" class="profile-actions">
            <el-button round @click="openEdit">编辑资料</el-button>
          </div>
          <div v-else-if="userStore.isLoggedIn" class="profile-actions">
            <el-button
              :type="following ? 'default' : 'primary'"
              :loading="actionLoading"
              round
              @click="handleToggleFollow"
            >
              {{ following ? '已关注' : '加关注' }}
            </el-button>
            <el-button
              v-if="friendStatus === 'NONE'"
              :loading="actionLoading"
              round
              @click="handleAddFriend"
            >
              加好友
            </el-button>
            <el-tag v-else-if="friendStatus === 'PENDING'" type="warning" effect="light" round>已发送好友请求</el-tag>
            <el-tag v-else-if="friendStatus === 'ACCEPTED'" type="success" effect="light" round>已是好友</el-tag>
          </div>
        </div>

        <div class="posts-section">
          <h2 class="section-title">
            <span class="bar" />
            {{ profile.username }} 的帖子
          </h2>

          <el-empty v-if="!posts.length" description="还没有发过帖子" />

          <div v-else class="post-list">
            <div v-for="p in posts" :key="p.id" class="post-item" @click="goPost(p.id)">
              <div class="post-title">{{ p.title }}</div>
              <div class="post-snippet">{{ p.content }}</div>
              <div class="post-meta">
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
              :page-sizes="[5, 10, 20]"
              layout="total, sizes, prev, pager, next"
              background
            />
          </div>
        </div>
      </template>
    </div>
  </div>

  <el-dialog v-model="editDialog" title="编辑资料" width="420px" top="10vh">
    <el-form label-position="top">
      <el-form-item label="头像">
        <div class="avatar-upload-wrap">
          <el-avatar v-if="!editAvatar && !profile?.avatarUrl" :size="72" class="profile-avatar">{{ (profile?.username || '?').slice(0, 1) }}</el-avatar>
          <el-avatar v-else :size="72" :src="editAvatar || profile?.avatarUrl" class="profile-avatar-img" />
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*"
            :on-change="(u: any) => onAvatarUpload(u.raw)"
          >
            <el-button size="small" round>更换头像</el-button>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item label="个人签名">
        <el-input v-model="editBio" type="textarea" :rows="3" maxlength="256" show-word-limit placeholder="写一句话介绍自己" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button round @click="editDialog = false">取消</el-button>
      <el-button type="primary" round :loading="editLoading" @click="saveProfile">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.profile-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 920px;
  margin: 0 auto;
}

.profile-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-size: 28px;
  flex-shrink: 0;
}

.profile-avatar-img {
  flex-shrink: 0;
}

.profile-name {
  margin: 0 0 4px;
  font-size: 1.4rem;
  font-weight: 800;
}

.profile-no {
  font-size: 13px;
  color: var(--mf-muted);
}

.profile-bio {
  font-size: 13px;
  color: var(--mf-muted);
  margin-top: 6px;
  line-height: 1.4;
  max-width: 320px;
}

.profile-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 60px;
}

.stat-n {
  font-size: 1.3rem;
  font-weight: 800;
  color: var(--mf-text);
}

.stat-l {
  font-size: 12px;
  color: var(--mf-muted);
}

.profile-actions {
  display: flex;
  gap: 8px;
}

.posts-section {
  margin-top: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--mf-text);
}

.section-title .bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: var(--mf-primary);
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
  transition: background 0.12s ease;
}

.post-item:last-child {
  border-bottom: none;
}

.post-item:hover {
  background: #fff8f8;
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--mf-muted);
}

.time {
  color: #999;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.avatar-upload-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
