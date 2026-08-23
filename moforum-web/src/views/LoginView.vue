<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (!form.value.username.trim() || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const { data } = await login(form.value.username, form.value.password)
    if (data.success && data.userId != null && data.username) {
      localStorage.setItem('moforum_token', data.token || '')
      user.setSession(data.userId, data.username, data.userNo, data.avatarUrl)
      ElMessage.success(data.message)
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      await router.replace(redirect || '/')
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-logo">Mo</div>
    <h1 class="auth-title">登录</h1>
    <p class="auth-desc">欢迎回到 MoForum</p>
    <el-form label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="用户名 / Mo 号">
        <el-input v-model="form.username" placeholder="输入用户名或 Mo 号" size="large" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="输入密码" size="large" show-password />
      </el-form-item>
      <el-button type="primary" size="large" round native-type="submit" :loading="loading" class="auth-submit">
        登录
      </el-button>
    </el-form>
    <div class="auth-switch">
      还没有账号？<router-link to="/auth/register" class="auth-link">去注册</router-link>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  padding: 36px 30px 30px;
  background: var(--mf-paper);
  border: 1px solid var(--mf-border);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(28, 25, 23, 0.08), 0 2px 8px rgba(28, 25, 23, 0.04);
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  padding: 0 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, #b52b2b, #c0392b);
  color: #fff;
  font-family: 'Playfair Display', Georgia, serif;
  font-weight: 800;
  font-size: 1.4rem;
  letter-spacing: 0.06em;
  margin-bottom: 18px;
  box-shadow: 0 8px 20px rgba(181, 43, 43, 0.28);
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

.auth-submit {
  width: 100%;
}

.auth-switch {
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
</style>