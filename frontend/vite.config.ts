import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 개발 중에는 Vite 프록시로 백엔드(8080)에 요청을 넘겨 CORS 없이 동작한다.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/actuator': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
