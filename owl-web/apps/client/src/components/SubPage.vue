<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAssistantStore } from '@/stores/assistant'
import MallTools from './MallTools.vue'
import './subpage.css'

defineProps<{
  title: string
  /** 标题下的一行小字，如「共 3 件 · 合计 ¥398」 */
  subtitle?: string
  /** 底部操作条是否显示（购物车空态时不需要） */
  showFooter?: boolean
}>()

const ICONS = {
  owl: '<path d="M7 11.4 6.6 5.6l4.7 3.6"/><path d="M25 11.4l.4-5.8-4.7 3.6"/><path d="M16 6.6c5.6 0 9.4 4.3 9.4 9.7s-4 10.9-9.4 10.9-9.4-5.5-9.4-10.9S10.4 6.6 16 6.6Z"/><path d="M11.9 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M20.1 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M16 17.6l-1.4 2.2h2.8L16 17.6Z"/>',
}

const router = useRouter()
const assistant = useAssistantStore()

function back() {
  /**
   * 「←」一律一键回首页。
   * 之前是 history.back()，从购物车→个人中心→消息这样连点下来，得退三四次才回得去；
   * 现在只在「上一页正好是首页」时用 back()（历史更干净），其余情况直接推首页。
   */
  if (window.history.state?.back === '/') router.back()
  else void router.push({ name: 'home' })
}
</script>

<template>
  <div class="sub">
    <header class="sub-top">
      <div class="sub-top-inner">
        <button class="sub-back" type="button" title="返回首页" aria-label="返回首页" @click="back">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M14 6.5 8.5 12 14 17.5" />
          </svg>
        </button>

        <div class="sub-title">
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>

        <MallTools />

        <button
          class="sub-ai"
          type="button"
          title="打开 OWL 购物分析助手"
          @click="assistant.openPanel()"
        >
          <svg
            viewBox="0 0 32 32"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.owl"
          />
          <span>AI 助手</span>
        </button>
      </div>
    </header>

    <div class="sub-body">
      <div class="sub-inner">
        <slot />
      </div>
    </div>

    <div v-if="showFooter" class="sub-foot">
      <slot name="footer" />
    </div>
  </div>
</template>
