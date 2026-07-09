import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  define: { global: 'globalThis' },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/user': {
        target: 'http://localhost:8080', changeOrigin: true,
        bypass: (req) => { if (req.headers?.accept?.includes('text/html')) return '/index.html' },
      },
      '/post': { target: 'http://localhost:8080', changeOrigin: true },
      '/board': { target: 'http://localhost:8080', changeOrigin: true },
      '/reply': { target: 'http://localhost:8080', changeOrigin: true },
      '/search': {
        target: 'http://localhost:8080', changeOrigin: true,
        bypass: (req) => { if (req.headers?.accept?.includes('text/html')) return '/index.html' },
      },
      '/follow': { target: 'http://localhost:8080', changeOrigin: true },
      '/friend': {
        target: 'http://localhost:8080', changeOrigin: true,
        bypass: (req) => { if (req.headers?.accept?.includes('text/html')) return '/index.html' },
      },
      '/message': { target: 'http://localhost:8080', changeOrigin: true },
      '/upload': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin': {
        target: 'http://localhost:8080', changeOrigin: true,
        bypass: (req) => { if (req.headers?.accept?.includes('text/html')) return '/index.html' },
      },
      '/ws': { target: 'http://localhost:8080', ws: true, changeOrigin: true },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
        }
      }
    }
  }
})
