/// <reference types="vite/client" />

/**
 * 助手后端配置。
 * 留空 → 走 src/services/assistant.ts 里的本地演示数据（现在就是这个状态）；
 * 填上 → 自动切到真实大模型接口，UI 一行都不用改。
 *
 * 用法：在项目根建 .env.local，写
 *   VITE_ASSISTANT_API=https://你的服务/assistant
 *   VITE_ASSISTANT_MODEL=deepseek-chat
 */
interface ImportMetaEnv {
  readonly VITE_ASSISTANT_API?: string
  readonly VITE_ASSISTANT_MODEL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
