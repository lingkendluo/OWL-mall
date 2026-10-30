/**
 * 用户端公告接口（/c/notices，免鉴权）。
 *
 * 放在 shared 而不是管理端：首页顶栏要展示后台发布的公告，
 * 两端共用同一份契约，字段和后端 VO 保持一致。管理端的公告增删改在
 * apps/admin/src/services/admin.ts，不在这里。
 */
import { http, withQuery } from '../http'

/** 1 公告 2 活动 3 维护 4 上新 */
export type NoticeType = 1 | 2 | 3 | 4

export interface PublicNotice {
  id: string
  title: string
  body: string
  type: NoticeType
  typeText: string
  pinned: boolean
  time: string
  publishAt: number | null
}

export const noticeApi = {
  list: (limit = 5) => http.get<PublicNotice[]>(withQuery('/c/notices', { limit })),
}
