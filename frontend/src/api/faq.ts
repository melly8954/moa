import type {
  AdminFaq,
  AdminFaqListItem,
  AdminFaqQuery,
  Faq,
  FaqQuery,
  FaqSaveRequest,
  SupportCategory,
} from "@/types/faq"
import { http } from "./client"

// FAQ (specs/017-customer-center)
//   api.v1.faq: 사용자 FAQ와 고객센터 분류 (로그인 없이)
//   api.v1.faqAdmin: FAQ 관리 (권한 FAQ_MANAGE). 구성 목록이라 쪽 없이 전체를 받는다 (FR-015)
//   답의 이미지는 공지와 같이 rich-text 편집기가 올린다
const base = "/api/v1/faqs"
const adminBase = "/api/v1/admin/faqs"

export const faqApi = {
  list: (params?: FaqQuery) => http.get<Faq[]>(base, { params, skipAuth: true }).then((res) => res.data),
  categories: () => http.get<SupportCategory[]>(`${base}/categories`, { skipAuth: true }).then((res) => res.data),
}

export const faqAdminApi = {
  list: (params?: AdminFaqQuery) => http.get<AdminFaqListItem[]>(adminBase, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminFaq>(`${adminBase}/${id}`).then((res) => res.data),
  create: (data: FaqSaveRequest) => http.post<AdminFaq>(adminBase, data).then((res) => res.data),
  modify: (id: number, data: FaqSaveRequest) => http.put<AdminFaq>(`${adminBase}/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${adminBase}/${id}`).then(() => undefined),
  setEnabled: (ids: number[], enabled: boolean) =>
    http.put<void>(`${adminBase}/enabled`, { ids, enabled }).then(() => undefined),
  bulkDelete: (ids: number[]) => http.post<void>(`${adminBase}/bulk-delete`, { ids }).then(() => undefined),
}
