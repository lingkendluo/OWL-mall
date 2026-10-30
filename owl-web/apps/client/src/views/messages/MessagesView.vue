<script setup lang="ts">
import { computed, ref } from 'vue'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { useMessageStore, type Message, type MessageKind } from '@/stores/messages'
import './messages.css'

type TabKey = 'all' | MessageKind

const messages = useMessageStore()
const { show } = useToast()

const ICONS: Record<MessageKind, string> = {
  order:
    '<path d="M6.4 3.8h11.2v16.4l-2.8-1.6-2.8 1.6-2.8-1.6-2.8 1.6V3.8Z"/><path d="M9.4 8.6h5.2"/><path d="M9.4 12.2h5.2"/>',
  promo:
    '<path d="M4 9.6V7.2h16v2.4a2.4 2.4 0 0 0 0 4.8v2.4H4v-2.4a2.4 2.4 0 0 0 0-4.8Z"/><path d="M12 8.4v7.4"/>',
  system:
    '<path d="M12 20.4a8.4 8.4 0 1 0 0-16.8 8.4 8.4 0 0 0 0 16.8Z"/><path d="M12 8.2v4.6"/><path d="M12 15.8h.01"/>',
}

const TABS: Array<{ key: TabKey; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'order', label: '订单' },
  { key: 'promo', label: '优惠' },
  { key: 'system', label: '系统' },
]

const active = ref<TabKey>('all')

const unreadOf = (key: TabKey) =>
  key === 'all' ? messages.unread : messages.unreadOf(key as MessageKind)

const list = computed<Message[]>(() =>
  active.value === 'all'
    ? messages.list
    : messages.list.filter((item) => item.kind === active.value),
)

const subtitle = computed(() =>
  messages.unread ? `${messages.unread} 条未读` : '全部已读',
)

function open(item: Message) {
  if (item.read) return
  messages.markRead(item.id)
}

function remove(item: Message) {
  messages.remove(item.id)
  show('已删除该条消息')
}

function markAll() {
  if (!messages.unread) {
    show('没有未读消息')
    return
  }
  messages.markAllRead()
  show('已全部标为已读')
}
</script>

<template>
  <SubPage title="消息" :subtitle="subtitle">
    <div class="msg">
      <!-- 分类 tab -->
      <div class="msg-bar">
        <div class="msg-tabs" role="tablist">
          <button
            v-for="tab in TABS"
            :key="tab.key"
            class="msg-tab"
            :class="{ 'is-active': active === tab.key }"
            type="button"
            role="tab"
            :aria-selected="active === tab.key"
            @click="active = tab.key"
          >
            {{ tab.label }}
            <em v-if="unreadOf(tab.key)">{{ unreadOf(tab.key) }}</em>
          </button>
        </div>

        <button class="msg-readall" type="button" @click="markAll">全部已读</button>
      </div>

      <!-- 列表 -->
      <ul v-if="list.length" class="msg-list">
        <li
          v-for="item in list"
          :key="item.id"
          class="msg-item"
          :class="{ 'is-unread': !item.read }"
        >
          <button class="msg-main" type="button" @click="open(item)">
            <span class="msg-ico" :class="`msg-ico--${item.kind}`">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS[item.kind]"
              />
            </span>

            <span class="msg-body">
              <span class="msg-head">
                <b class="msg-title">{{ item.title }}</b>
                <em class="msg-time">{{ item.time }}</em>
              </span>
              <span class="msg-text">{{ item.body }}</span>
            </span>

            <i v-if="!item.read" class="msg-dot" />
          </button>

          <button class="msg-del" type="button" aria-label="删除消息" @click="remove(item)">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
            >
              <path d="M6.2 6.2l11.6 11.6M17.8 6.2 6.2 17.8" />
            </svg>
          </button>
        </li>
      </ul>

      <!-- 空态 -->
      <section v-else class="msg-empty">
        <span class="msg-empty-ico">✉️</span>
        <h2>这里还很安静</h2>
        <p>「{{ TABS.find((t) => t.key === active)?.label }}」分类下暂时没有消息</p>
      </section>
    </div>
  </SubPage>

  <AppToast />
</template>
