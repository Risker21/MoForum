<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-bg" aria-hidden="true" />
    <div class="auth-card-wrap">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.auth-wrap {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  position: relative;
  overflow: hidden;
  background: var(--mf-bg);
}

.auth-bg {
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(circle at 15% 25%, rgba(198, 40, 40, 0.18), transparent 42%),
    radial-gradient(circle at 85% 15%, rgba(255, 111, 0, 0.14), transparent 38%),
    radial-gradient(circle at 50% 85%, rgba(74, 20, 140, 0.1), transparent 45%);
  pointer-events: none;
}

.auth-card-wrap {
  position: relative;
  width: 100%;
  max-width: 420px;
}
</style>
