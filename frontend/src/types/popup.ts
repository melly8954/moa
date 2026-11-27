import type { ExposureStatus } from "@/types/marketing"

// 팝업 관리 API의 응답·요청 (specs/016-banner-popup contracts/admin-api.md)

export type AdminPopupListItem = {
  id: number
  title: string
  startsAt: string
  endsAt?: string | null
  sortOrder: number
  enabled: boolean
  status: ExposureStatus
}

export type AdminPopup = AdminPopupListItem & {
  content: string
  linkUrl?: string | null
  createdAt: string
  updatedAt: string
}

export type PopupSaveRequest = {
  title: string
  content: string
  linkUrl?: string
  startsAt: string
  endsAt?: string
  sortOrder: number
  enabled: boolean
}
