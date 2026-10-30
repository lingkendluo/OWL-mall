/// <reference types="vite/client" />

/**
 * 管理端的接口前缀。
 * 默认留空 → 走 vite 开发代理（/api/v1 → http://localhost:8080）。
 * 部署到别的域名时在 .env.local 里填 VITE_API_BASE=https://api.example.com/api/v1
 */
interface ImportMetaEnv {
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
