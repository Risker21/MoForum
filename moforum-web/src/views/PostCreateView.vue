<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBoard, type BoardRow } from '@/api/board'
import { createPost } from '@/api/post'
import { uploadImage, confirmImage } from '@/api/upload'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const boardId = computed(() => Number(route.params.boardId))
const board = ref<BoardRow | null>(null)

const title = ref('')
const content = ref('')
const loading = ref(false)
const uploadingImg = ref(false)

onMounted(async () => {
  if (!Number.isFinite(boardId.value) || boardId.value < 1) {
    ElMessage.error('无效的吧')
    router.replace({ name: 'home' })
    return
  }
  try {
    const { data } = await getBoard(boardId.value)
    board.value = data ?? null
    if (!board.value?.id) {
      ElMessage.warning('贴吧不存在')
      router.replace({ name: 'home' })
    }
  } catch {
    router.replace({ name: 'home' })
  }
})

const fileInput = ref<HTMLInputElement | null>(null)

function triggerFileInput() {
  fileInput.value?.click()
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input?.files?.[0]
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
  uploadingImg.value = true
  try {
    const { data } = await uploadImage(file)
    if (data.success && data.url) {
      content.value += `\n![image](${data.url})\n`
      confirmImage(data.url)
      ElMessage.success('图片已插入')
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    uploadingImg.value = false
  }
}

async function onSubmit() {
  if (user.userId == null) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!title.value.trim() || !content.value.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  loading.value = true
  try {
    const { data } = await createPost({
      boardId: boardId.value,
      title: title.value.trim(),
      content: content.value.trim(),
    })
    if (data.success) {
      ElMessage.success(data.message)
      await router.push({ name: 'board', params: { boardId: String(boardId.value) } })
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="create-page">
    <div class="inner">
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ name: 'home' }">吧广场</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'board', params: { boardId: String(boardId) } }">
          {{ board?.name || '贴吧' }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>发帖</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="panel mf-card">
        <h2 class="mf-page-title">发布新帖</h2>
        <p class="mf-page-sub">正在发布到「{{ board?.name || '…' }}」</p>

        <el-form label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="标题">
            <el-input v-model="title" maxlength="255" show-word-limit placeholder="起一个吸引人的标题" />
          </el-form-item>
          <el-form-item label="正文">
            <div class="editor-toolbar">
              <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileSelected" />
              <el-button size="small" :loading="uploadingImg" @click="triggerFileInput">
                {{ uploadingImg ? '上传中…' : '📷 插入图片' }}
              </el-button>
            </div>
            <el-input v-model="content" type="textarea" :rows="12" placeholder="正文支持多行，注意友善交流" />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" size="large" round :loading="loading" native-type="submit">发布</el-button>
            <el-button round @click="router.back()">取消</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-page {
  padding: 20px 16px 48px;
}

.inner {
  max-width: 720px;
  margin: 0 auto;
}

.crumb {
  margin-bottom: 16px;
}

.panel {
  padding: 24px 26px;
}

.editor-toolbar {
  margin-bottom: 8px;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
</style>
