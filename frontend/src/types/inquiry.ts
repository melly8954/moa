// 1:1 문의 API의 응답·요청 (specs/017-customer-center contracts/api.md)

export type InquiryStatus = "WAITING" | "ANSWERED"

export type MyInquiryListItem = {
  id: number
  categoryCode: string
  categoryName: string
  title: string
  status: InquiryStatus
  createdAt: string
}

export type MyInquiry = MyInquiryListItem & {
  content: string
  imageUrls: string[]
  answer?: string | null
  answeredAt?: string | null
  deletable: boolean
}

export type InquiryCreateRequest = {
  categoryCode: string
  title: string
  content: string
  imageUrls: string[]
}

export type InquiryAuthor = {
  userId: number
  loginId?: string | null
  name?: string | null
}

export type AdminInquiryListItem = {
  id: number
  status: InquiryStatus
  categoryCode: string
  categoryName: string
  title: string
  author?: InquiryAuthor | null
  createdAt: string
  answeredAt?: string | null
}

export type AdminInquiry = AdminInquiryListItem & {
  content: string
  imageUrls: string[]
  answer?: string | null
  answeredByName?: string | null
  answerable: boolean
}

export type AdminInquiryQuery = {
  // 비면 답변 대기, ALL이면 모두 (research R6)
  status?: string
  categoryCode?: string
  from?: string
  to?: string
  keyword?: string
}
