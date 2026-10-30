<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { QUICK_PROMPTS, REMOTE_ENABLED } from '@/services/assistant'
import { useAssistantStore } from '@/stores/assistant'
import { useCartStore } from '@/stores/cart'
import './assistant.css'

const assistant = useAssistantStore()
const cart = useCartStore()

const ICONS = {
  owl: '<path d="M7 11.4 6.6 5.6l4.7 3.6"/><path d="M25 11.4l.4-5.8-4.7 3.6"/><path d="M16 6.6c5.6 0 9.4 4.3 9.4 9.7s-4 10.9-9.4 10.9-9.4-5.5-9.4-10.9S10.4 6.6 16 6.6Z"/><path d="M11.9 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M20.1 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M16 17.6l-1.4 2.2h2.8L16 17.6Z"/>',
  close: '<path d="M6.5 6.5l11 11"/><path d="M17.5 6.5l-11 11"/>',
  clear:
    '<path d="M4.2 6.8h15.6"/><path d="M9.4 6.8V4.6h5.2v2.2"/><path d="M6.6 6.8l.9 12.3a2 2 0 0 0 2 1.9h5a2 2 0 0 0 2-1.9l.9-12.3"/>',
  send: '<path d="M12 19.2V5.4"/><path d="M6.2 11.2 12 5.4l5.8 5.8"/>',
}

const draft = ref('')
const listEl = ref<HTMLElement | null>(null)
const inputEl = ref<HTMLTextAreaElement | null>(null)

const status = computed(() => {
  const state = REMOTE_ENABLED ? '在线' : '演示模式'
  const reading = cart.count ? `已读购物车 ${cart.count} 件` : '随时问我'
  return `${state} · ${reading}`
})

const note = 'Enter 发送 · Shift + Enter 换行'

const canSend = computed(() => draft.value.trim().length > 0 && !assistant.sending)

async function scrollToEnd() {
  await nextTick()
  const box = listEl.value
  if (box) box.scrollTop = box.scrollHeight
}

function autoGrow() {
  const el = inputEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(120, el.scrollHeight)}px`
}

function submit() {
  if (!canSend.value) return
  const text = draft.value
  draft.value = ''
  void nextTick(autoGrow)
  void assistant.send(text)
}

function ask(prompt: string) {
  if (assistant.sending) return
  void assistant.send(prompt)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  // 中文输入法选词时的回车不能当发送
  if (event.isComposing) return
  event.preventDefault()
  submit()
}

function onWindowKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && assistant.open) assistant.closePanel()
}

watch(
  () => assistant.messages.length,
  () => void scrollToEnd(),
)

watch(
  () => assistant.sending,
  () => void scrollToEnd(),
)

watch(
  () => assistant.open,
  (open) => {
    if (!open) return
    void scrollToEnd()
    void nextTick(() => inputEl.value?.focus())
  },
)

onMounted(() => window.addEventListener('keydown', onWindowKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onWindowKey))
</script>

<template>
  <div class="ai-root" :class="{ 'is-open': assistant.open }">
    <div class="ai-scrim" @click="assistant.closePanel()" />

    <aside class="ai-panel" role="dialog" aria-modal="true" aria-label="OWL 购物分析助手">
      <header class="ai-head">
        <span class="ai-mark">
          <svg
            viewBox="0 0 32 32"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.owl"
          />
        </span>
        <div class="ai-id">
          <b>OWL 购物分析助手</b>
          <small><i />{{ status }}</small>
        </div>
        <button
          class="ai-icon"
          type="button"
          title="清空对话"
          aria-label="清空对话"
          @click="assistant.clear()"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.clear"
          />
        </button>
        <button
          class="ai-icon"
          type="button"
          title="关闭"
          aria-label="关闭"
          @click="assistant.closePanel()"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.close"
          />
        </button>
      </header>

      <div ref="listEl" class="ai-list">
        <div class="ai-thread">
          <article
            v-for="msg in assistant.messages"
            :key="msg.id"
            class="ai-row"
            :class="`is-${msg.role}`"
          >
            <span v-if="msg.role === 'assistant'" class="ai-mini">
              <svg
                viewBox="0 0 32 32"
                fill="none"
                stroke="currentColor"
                stroke-width="1.9"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.owl"
              />
            </span>
            <p class="ai-bubble">{{ msg.text }}</p>
          </article>

          <article v-if="assistant.sending" class="ai-row is-assistant">
            <span class="ai-mini">
              <svg
                viewBox="0 0 32 32"
                fill="none"
                stroke="currentColor"
                stroke-width="1.9"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.owl"
              />
            </span>
            <p class="ai-bubble ai-typing"><i /><i /><i /></p>
          </article>

          <div v-if="!assistant.ready" class="ai-asks">
            <p class="ai-asks-label">试着这样问</p>
            <button
              v-for="prompt in QUICK_PROMPTS"
              :key="prompt"
              class="ai-chip"
              type="button"
              @click="ask(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </div>
      </div>

      <footer class="ai-foot">
        <div class="ai-composer">
          <textarea
            ref="inputEl"
            v-model="draft"
            rows="1"
            placeholder="问我：帮我凑到免运费"
            aria-label="给助手发消息"
            @input="autoGrow"
            @keydown="onKeydown"
          />
          <button
            class="ai-send"
            type="button"
            aria-label="发送"
            :disabled="!canSend"
            @click="submit"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.9"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="ICONS.send"
            />
          </button>
        </div>
        <p class="ai-note">{{ note }}</p>
      </footer>
    </aside>
  </div>
</template>
