<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPostDetail, deletePost, type PostRow } from '@/api/post'
import { listReplies, createReply, type ReplyRow } from '@/api/reply'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const postId = computed(() => Number(route.params.id))
const post = ref<PostRow | null>(null)
const replies = ref<ReplyRow[]>([])
const loading = ref(true)
const replyText = ref('')
const submitting = ref(false)
const deleting = ref(false)

function goUser(id: number) {
  router.push({ name: 'user-profile', params: { userId: String(id) } })
}

const isOwner = computed(
  () => user.userId != null && post.value?.userId != null && user.userId === post.value.userId,
)

async function load() {
  if (!Number.isFinite(postId.value) || postId.value < 1) {
    ElMessage.error('无效的帖子')
    return
  }
  loading.value = true
  post.value = null
  replies.value = []

  const [p, r] = await Promise.all([
    getPostDetail(postId.value).then(r => r.data).catch(() => null as PostRow | null),
    listReplies(postId.value).then(r => r.data).catch(() => [] as ReplyRow[]),
  ])

  post.value = p
  if (!p) ElMessage.error('帖子不存在或已删除')
  replies.value = r
  loading.value = false
}

onMounted(load)
watch(postId, load)

async function submitReply() {
  if (!user.isLoggedIn) {
    ElMessage.warning('请先登录再盖楼')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const t = replyText.value.trim()
  if (!t) {
    ElMessage.warning('写点内容再发')
    return
  }
  if (user.userId == null) return
  submitting.value = true
  try {
    const { data } = await createReply({
      postId: postId.value,
      content: t,
    })
    if (data.success) {
      ElMessage.success(data.message)
      replyText.value = ''
      await load()
    } else {
      ElMessage.error(data.message || '回复失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '回复失败')
  } finally {
    submitting.value = false
  }
}

async function removePost() {
  if (!isOwner.value || user.userId == null) return
  try {
    await ElMessageBox.confirm('删除后本帖及所有回复将不可恢复，确定删除？', '删帖', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    const { data } = await deletePost(postId.value)
    if (data.success) {
      ElMessage.success(data.message)
      if (post.value?.boardId) {
        await router.replace({ name: 'board', params: { boardId: String(post.value.boardId) } })
      } else {
        await router.replace({ name: 'home' })
      }
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="detail-page">
    <div class="inner">
      <el-skeleton v-if="loading" :rows="8" animated />

      <template v-else-if="post">
        <el-breadcrumb class="crumb" separator="/">
          <el-breadcrumb-item :to="{ name: 'home' }">广场</el-breadcrumb-item>
          <el-breadcrumb-item
            v-if="post.boardId"
            :to="{ name: 'board', params: { boardId: String(post.boardId) } }"
          >
            {{ post.boardName || '贴吧' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>帖子</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="mf-card floor main-floor">
          <div class="floor-tag">#1 楼主</div>
          <div class="floor-head">
            <el-avatar v-if="!post.avatarUrl" class="av">{{ post.authorName?.slice(0, 1) || '匿' }}</el-avatar>
            <el-avatar v-else class="av" :src="post.avatarUrl" />
            <div>
              <div class="name link" @click.stop="goUser(post.userId)">{{ post.authorName || '匿名用户' }}</div>
              <div class="time">{{ post.createTime }}</div>
            </div>
            <div class="stats">
              <span>{{ post.viewCount ?? 0 }} 浏览</span>
              <span>{{ post.replyCount ?? 0 }} 回复</span>
              <el-button
                v-if="isOwner"
                type="danger"
                link
                size="small"
                :loading="deleting"
                @click.stop="removePost"
              >
                删除
              </el-button>
            </div>
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-body">{{ post.content }}</div>
        </div>

        <h2 class="reply-title">全部回复（{{ replies.length }}）</h2>

        <div v-if="!replies.length" class="no-reply">暂无回复，快来抢沙发～</div>

        <div
          v-for="(r, idx) in replies"
          :key="r.id"
          class="mf-card floor reply-floor"
        >
          <div class="floor-tag">#{{ idx + 2 }}</div>
          <div class="floor-head">
            <el-avatar v-if="!r.avatarUrl" class="av sm">{{ r.authorName?.slice(0, 1) || '匿' }}</el-avatar>
            <el-avatar v-else class="av sm" :src="r.avatarUrl" />
            <div>
              <div class="name link" @click.stop="goUser(r.userId)">{{ r.authorName || '匿名' }}</div>
              <div class="time">{{ r.createTime }}</div>
            </div>
          </div>
          <div class="reply-body">{{ r.content }}</div>
        </div>

        <div class="mf-card composer">
          <h3 class="composer-title">发表回复</h3>
          <el-input
            v-model="replyText"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="文明发言，理性讨论…"
          />
          <div class="composer-actions">
            <el-button type="primary" round :loading="submitting" @click="submitReply">发表</el-button>
          </div>
        </div>
      </template>

      <el-empty v-else description="帖子不存在">
        <el-button type="primary" round @click="router.push({ name: 'home' })">回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 800px;
  margin: 0 auto;
}

.crumb {
  margin-bottom: 16px;
  font-size: 13px;
}

.floor {
  padding: 20px 22px;
  margin-bottom: 14px;
}

.main-floor {
  background: linear-gradient(180deg, #fffefb 0%, #ffffff 40%);
}

.floor-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--mf-primary);
  margin-bottom: 10px;
  letter-spacing: 0.04em;
}

.floor-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.floor-head .stats {
  margin-left: auto;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--mf-muted);
  align-items: center;
}

.av {
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-size: 16px;
}

.av.sm {
  width: 36px !important;
  height: 36px !important;
  font-size: 14px;
}

.name {
  font-weight: 700;
  font-size: 14px;
}

.link { cursor: pointer; }
.link:hover { color: var(--mf-primary); }

.time {
  font-size: 12px;
  color: var(--mf-muted);
}

.post-title {
  margin: 0 0 14px;
  font-size: 1.4rem;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: 0.02em;
}

.post-body {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--mf-text);
}

.reply-title {
  margin: 22px 0 12px;
  font-size: 1rem;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.no-reply {
  padding: 16px;
  text-align: center;
  color: var(--mf-muted);
  font-size: 14px;
  background: var(--mf-paper);
  border-radius: 12px;
  border: 1px dashed var(--mf-border);
  margin-bottom: 14px;
}

.reply-floor .reply-body {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.composer {
  margin-top: 8px;
  padding: 18px 22px;
}

.composer-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 700;
}

.composer-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
