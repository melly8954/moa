// FAQ API의 응답·요청 (specs/017-customer-center contracts/api.md)

// 고객센터 분류 (지금 고를 수 있는 항목, GET /api/v1/faqs/categories)
export type SupportCategory = {
  code: string
  name: string
}

// 사용자 FAQ 항목. 펼칠 때 따로 부르지 않도록 답까지 온다
export type Faq = {
  id: number
  categoryCode: string
  categoryName: string
  question: string
  answer: string
}

export type FaqQuery = {
  categoryCode?: string
  keyword?: string
}

export type AdminFaqListItem = {
  id: number
  categoryCode: string
  categoryName: string
  question: string
  sortOrder: number
  enabled: boolean
  updatedAt: string
}

export type AdminFaq = AdminFaqListItem & {
  answer: string
  createdAt: string
}

export type AdminFaqQuery = {
  categoryCode?: string
  // "true"·"false", 비면 모두
  enabled?: string
  keyword?: string
}

export type FaqSaveRequest = {
  categoryCode: string
  question: string
  answer: string
  sortOrder: number
  enabled: boolean
}
