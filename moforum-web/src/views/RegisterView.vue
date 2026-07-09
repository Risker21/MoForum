<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  password2: '',
})

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  if (form.password !== form.password2) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const { data } = await register(form.username, form.password)
    if (data.success) {
      if (data.token && data.userId != null && data.username) {
        localStorage.setItem('moforum_token', data.token)
        user.setSession(data.userId, data.username, data.userNo, data.avatarUrl)
        ElMessage.success(data.message + '，已自动登录')
        await router.replace({ name: 'home' })
      } else {
        const mo = data.userNo != null ? `，您的 Mo 号：${data.userNo}（可用于登录、寻人）` : ''
        ElMessage.success(data.message + mo)
        await router.replace({ name: 'login' })
      }
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-card mf-card">
      <h1 class="auth-title">创建账号</h1>
      <p class="auth-desc">加入 MoForum，开始讨论</p>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="form.password2"
            type="password"
            placeholder="请再次输入密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-button type="primary" size="large" round :loading="loading" native-type="submit" style="width:100%">
          注册
        </el-button>
      </el-form>
      <div class="auth-footer">
        已有账号？<router-link to="/auth/login" class="auth-link">去登录</router-link>
      </div>
    </div>
    <p class="auth-brand">MoForum · 方寸之间，自有天地</p>
  </div>
</template>

<style scoped>
.auth-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 20px;
}

.auth-card {
  width: 380px;
  max-width: 100%;
  padding: 36px 28px 28px;
}

.auth-title {
  margin: 0 0 2px;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.auth-desc {
  margin: 0 0 24px;
  color: var(--mf-muted);
  font-size: 14px;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: var(--mf-muted);
}

.auth-link {
  color: var(--mf-primary);
  font-weight: 600;
}
.auth-link:hover {
  text-decoration: underline;
}

.auth-brand {
  margin-top: 24px;
  font-size: 12px;
  color: var(--mf-muted);
  letter-spacing: 0.06em;
}
</style>
