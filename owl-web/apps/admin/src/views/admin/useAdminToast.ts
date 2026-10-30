import { onBeforeUnmount, ref } from 'vue'

/**
 * 管理端的小提示条。写操作成功/失败都在页面顶部飘一句，
 * 3 秒自动消失；比 alert 不打断操作，比静默失败好排查。
 */
export function useAdminToast() {
  const message = ref('')
  const isError = ref(false)
  let timer: ReturnType<typeof setTimeout> | undefined

  function show(text: string, error = false) {
    message.value = text
    isError.value = error
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      message.value = ''
    }, 3000)
  }

  onBeforeUnmount(() => {
    if (timer) clearTimeout(timer)
  })

  return { message, isError, show }
}
