/**
 * 把 src/views/home/catalog.ts 导出成后端可读的 catalog.json。
 *
 * 为什么不用 tsx / node 直接跑：
 *   catalog.ts 里用了 import.meta.glob 索引 105 张商品图，普通 Node 不认识这个语法。
 *   这里借 Vite 的 SSR 模块加载器来跑，它天然支持 glob、TS 与路径别名。
 *
 * 用法：在 owl-web 目录下
 *   node scripts/export-catalog.mjs
 * 产出：
 *   ../docs/catalog.json   （再复制到 owl-server/owl-boot/src/main/resources/seed/）
 */
import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const here = dirname(fileURLToPath(import.meta.url))
// 拆分后 catalog.ts 在 apps/client 下，root 指到那儿，catalog.ts 里的
// import.meta.glob('../assets/products/*') 才能按应用内的相对路径解析
const root = resolve(here, '..', 'apps', 'client')

const server = await createServer({
  root,
  logLevel: 'error',
  server: { middlewareMode: true },
  appType: 'custom',
})

try {
  const mod = await server.ssrLoadModule('/src/views/home/catalog.ts')
  const categories = mod.CATEGORIES
  if (!Array.isArray(categories) || categories.length === 0) {
    throw new Error('CATEGORIES 为空，检查 catalog.ts 的导出')
  }

  const out = categories.map((cat, ci) => ({
    code: cat.id,
    name: cat.name,
    hint: cat.hint,
    glyph: cat.glyph,
    sort: ci * 10,
    subs: cat.subs.map((s, si) => ({ code: s.id, name: s.name, sort: si * 10 })),
    products: cat.items.map((p, pi) => ({
      code: p.id,
      name: p.name,
      spec: p.spec,
      price: p.price,
      list: p.list,
      tag: p.tag,
      emoji: p.emoji,
      sub: p.sub,
      sort: pi * 10,
      detail: p.detail,
    })),
  }))

  const target = resolve(root, '../docs/catalog.json')
  mkdirSync(dirname(target), { recursive: true })
  writeFileSync(target, JSON.stringify(out, null, 2), 'utf8')

  const total = out.reduce((n, c) => n + c.products.length, 0)
  const subs = out.reduce((n, c) => n + c.subs.length, 0)
  console.log(`catalog exported: ${out.length} categories / ${subs} subs / ${total} products`)
  console.log(`-> ${target}`)
} finally {
  await server.close()
}
