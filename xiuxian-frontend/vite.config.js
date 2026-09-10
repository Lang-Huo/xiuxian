import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发期将 /api 代理到 Spring Boot (8080)，规避跨域
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
