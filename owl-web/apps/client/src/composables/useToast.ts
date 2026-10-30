import { ref } from 'vue'

/**
 * 极简全局 toast：模块级单例，多个页面共用同一条。
 * 只用于演示态的轻提示，接后端后可换成消息中心。
 */
const text = ref('')
let timer: number | undefined

export function useToast() {
  function show(message: string) {
    text.value = message
    window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      text.value = ''
    }, 2200)
  }

  return { text, show }
}
