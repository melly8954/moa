import type { ExposureStatus } from "@/types/marketing"

// 배너 관리 API의 응답·요청 (specs/016-banner-popup contracts/admin-api.md)

export type AdminBannerListItem = {
  id: number
  name: string
  wideImageUrl: string
  startsAt: string
  endsAt?: string | null
  sortOrder: number
  enabled: boolean
  status: ExposureStatus
}

export type AdminBanner = AdminBannerListItem & {
  narrowImageUrl?: string | null
  altText: string
  linkUrl?: string | null
  createdAt: string
  updatedAt: string
}

// 이미지 주소는 rich-text 이미지 올리기(api.v1.richText.uploadImage)의 url
export type BannerSaveRequest = {
  name: string
  wideImageUrl: string
  narrowImageUrl?: string
  altText: string
  linkUrl?: string
  startsAt: string
  endsAt?: string
  sortOrder: number
  enabled: boolean
}
