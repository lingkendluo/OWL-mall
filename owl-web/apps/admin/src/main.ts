/**
 * 管理端入口（http://localhost:5174）。
 * 与用户端（apps/client，5173）完全独立：自己的 HTML、路由、Pinia 实例，
 * 只共用 owl-web/shared/styles 里的设计变量和基线样式。
 */
import '@shared/styles/tokens.css'
import '@shared/styles/base.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
