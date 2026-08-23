<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-bg" aria-hidden="true">
      <span class="blob blob-1" />
      <span class="blob blob-2" />
      <span class="blob blob-3" />
    </div>
    <div class="auth-card-wrap">
      <RouterView />
    </div>
    <footer class="auth-brand">MoForum · 方寸之间，自有天地</footer>
  </div>
</template>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  position: relative;
  overflow: hidden;
  background: var(--mf-bg);
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  animation: auth-float 14s ease-in-out infinite;
}

.blob-1 {
  width: 340px;
  height: 340px;
  background: rgba(198, 40, 40, 0.20);
  top: -90px;
  left: -70px;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: rgba(255, 111, 0, 0.16);
  bottom: -70px;
  right: -60px;
  animation-delay: -5s;
}

.blob-3 {
  width: 220px;
  height: 220px;
  background: rgba(74, 20, 140, 0.12);
  top: 42%;
  left: 62%;
  animation-delay: -9s;
}

@keyframes auth-float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-32px) scale(1.06); }
}

.auth-card-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  margin: auto 0;
  animation: auth-in 0.5s ease both;
}

@keyframes auth-in {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-brand {
  position: relative;
  z-index: 1;
  margin-top: 24px;
  font-size: 12px;
  color: var(--mf-muted);
  letter-spacing: 0.06em;
  text-align: center;
}
</style>
