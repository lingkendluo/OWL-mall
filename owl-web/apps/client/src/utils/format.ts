/** 金额展示：整数不带小数，非整数保留一位 */
export function money(value: number): string {
  const digits = Number.isInteger(value) ? 0 : 1
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })
}

/** 两位小数取整，避免 0.1 + 0.2 的浮点尾巴 */
export function round2(value: number): number {
  return Math.round(value * 100) / 100
}
