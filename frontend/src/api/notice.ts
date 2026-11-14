import type { PageRequest, PageResponse } from "@/types/api"
import type {
  AdminNotice,
  AdminNoticeListItem,
  AdminNoticeQuery,
  Notice,
  NoticeCreateRequest,
  NoticeListItem,
  NoticeModifyRequest,
  NoticeNew,
} from "@/types/notice"
import { http } from "./client"

// 공지 (specs/006-notices)
//   api.v1.notice: 사용자 목록·상세(로그인 없이), 새 공지 여부·확인(로그인)
//   api.v1.noticeAdmin: 공지 관리 (권한 NOTICE_MANAGE)
const base = "/api/v1/notices"
const adminBase = "/api/v1/admin/notices"

export const noticeApi = {
  getPage: (params?: PageRequest) =>
    http.get<PageResponse<NoticeListItem>>(base, { params, skipAuth: true }).then((res) => res.data),
  get: (id: number) => http.get<Notice>(`${base}/${id}`, { skipAuth: true }).then((res) => res.data),
  hasNew: () => http.get<NoticeNew>(`${base}/new`).then((res) => res.data),
  markSeen: () => http.put<void>(`${base}/seen`).then(() => undefined),
}

export const noticeAdminApi = {
  getPage: (params?: AdminNoticeQuery & PageRequest) =>
    http.get<PageResponse<AdminNoticeListItem>>(adminBase, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminNotice>(`${adminBase}/${id}`).then((res) => res.data),
  create: (data: NoticeCreateRequest) => http.post<AdminNotice>(adminBase, data).then((res) => res.data),
  modify: (id: number, data: NoticeModifyRequest) =>
    http.put<AdminNotice>(`${adminBase}/${id}`, data).then((res) => res.data),
  publish: (id: number) => http.post<AdminNotice>(`${adminBase}/${id}/publish`).then((res) => res.data),
  pin: (id: number, pinned: boolean) =>
    http.put<AdminNotice>(`${adminBase}/${id}/pin`, { pinned }).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${adminBase}/${id}`).then(() => undefined),
}
