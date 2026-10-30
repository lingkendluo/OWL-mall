<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AssistantPanel from '@/components/AssistantPanel.vue'
import { useAssistantStore } from '@/stores/assistant'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const assistant = useAssistantStore()
const auth = useAuthStore()

/** 登录页是独立视觉，不挂助手抽屉 */
const showAssistant = computed(() => route.name !== 'login')

// 联调：登录身份一变就重置对话 —— 退出时清干净，登录时用新昵称重新打招呼
watch(
  () => auth.isLoggedIn,
  () => assistant.clear(),
)

onMounted(() => {
  // 刷新页面后拉一次最新资料（积分、余额可能变了），失败静默
  if (auth.isLoggedIn) void auth.refreshMe()

  // 分享/调试用：带上 ?assistant=1 直接打开助手面板（登录页不生效）
  if (!showAssistant.value) return
  if (new URLSearchParams(window.location.search).get('assistant') === '1') {
    assistant.openPanel()
  }
})
</script>

<template>
  <RouterView v-slot="{ Component }">
    <component :is="Component" />
  </RouterView>
  <AssistantPanel v-if="showAssistant" />
</template>
