import type { PageRequest, PageResponse } from "@/types/api"
import type {
  AdminInquiry,
  AdminInquiryListItem,
  AdminInquiryQuery,
  InquiryCreateRequest,
  MyInquiry,
  MyInquiryListItem,
} from "@/types/inquiry"
import { http } from "./client"

// 1:1 문의 (specs/017-customer-center)
//   api.v1.inquiry: 회원 문의 (로그인. 관리자 계정은 A005)
//   api.v1.inquiryAdmin: 문의 관리 (권한 INQUIRY_MANAGE)
//   첨부 이미지는 api.v1.richText.uploadImage로 한 장씩 올리고 받은 주소를 imageUrls에 넣는다 (research R1)
const base = "/api/v1/inquiries"
const adminBase = "/api/v1/admin/inquiries"

export const inquiryApi = {
  myPage: (params?: PageRequest) =>
    http.get<PageResponse<MyInquiryListItem>>(`${base}/me`, { params }).then((res) => res.data),
  get: (id: number) => http.get<MyInquiry>(`${base}/${id}`).then((res) => res.data),
  create: (data: InquiryCreateRequest) => http.post<MyInquiry>(base, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${base}/${id}`).then(() => undefined),
}

export const inquiryAdminApi = {
  getPage: (params?: AdminInquiryQuery & PageRequest) =>
    http.get<PageResponse<AdminInquiryListItem>>(adminBase, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminInquiry>(`${adminBase}/${id}`).then((res) => res.data),
  answer: (id: number, content: string) =>
    http.put<void>(`${adminBase}/${id}/answer`, { content }).then(() => undefined),
}
