/// <reference types="vite/client" />

/**
 * 用户端环境变量。
 *
 * 助手后端（不填就走本地演示数据）：
 *   VITE_ASSISTANT_API=https://你的服务/assistant
 *   VITE_ASSISTANT_MODEL=deepseek-chat
 *
 * 后端接口前缀（不填就走 vite 代理 /api/v1 → localhost:8080）：
 *   VITE_API_BASE=https://api.example.com/api/v1
 */
interface ImportMetaEnv {
  readonly VITE_ASSISTANT_API?: string
  readonly VITE_ASSISTANT_MODEL?: string
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
