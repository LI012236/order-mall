import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发期把 /api 代理到后端，避免跨域配置
      '/api': {
        target: 'http://127.0.0.1:18091',
        changeOrigin: true
      }
    }
  }
})
