// 공지 API의 응답·요청·조건 (specs/006-notices contracts/notices-api.md)

export type NoticeStatus = "DRAFT" | "PUBLISHED"

// 공지 관리 목록 항목
export type AdminNoticeListItem = {
  id: number
  title: string
  status: NoticeStatus
  pinned: boolean
  publishedAt?: string | null
  updatedAt: string
}

// 공지 관리 상세 (고치기 화면)
export type AdminNotice = {
  id: number
  title: string
  content: string
  status: NoticeStatus
  pinned: boolean
  publishedAt?: string | null
  createdAt: string
  updatedAt: string
}

// 모두 선택
export type AdminNoticeQuery = { keyword?: string; status?: string }

export type NoticeCreateRequest = { title: string; content: string; pinned: boolean; publish: boolean }
export type NoticeModifyRequest = { title: string; content: string }

// 사용자 목록 항목과 상세 (게시한 공지만)
export type NoticeListItem = { id: number; title: string; pinned: boolean; publishedAt: string }
export type Notice = NoticeListItem & { content: string }

export type NoticeNew = { hasNew: boolean }
