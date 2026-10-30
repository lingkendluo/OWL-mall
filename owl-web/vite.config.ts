import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  /**
   * 管理端要直连 owl-server，开发期用代理绕开跨域和端口差异：
   *   /api/v1/**  → 后端所有接口（含 /admin/**、/c/**）
   *   /static/**  → 后端静态资源，商品图就挂在这里
   * 想直连别的主机时改 target，或用 VITE_API_BASE 覆盖前端请求前缀。
   */
  server: {
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/static': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
