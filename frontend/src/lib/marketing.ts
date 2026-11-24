import { formatDateTime } from "@/lib/format"
import type { ExposureStatusFilter } from "@/types/marketing"

// 배너·팝업 관리가 같이 쓰는 규칙 (specs/016-banner-popup research R5, R10)

export const LINK_URL_MAX = 500

// 연결 주소: 비우거나, /로 시작하되 //로 시작하지 않거나(다른 사이트로 가는 주소), http://·https://로 시작. 빈칸 없이
//   서버 MarketingRequests.LINK_URL_PATTERN과 같다
const LINK_URL = /^(\/(?!\/)\S*|https?:\/\/\S+)$/

export function isAllowedLinkUrl(value: string): boolean {
  if (value === "") return true
  return value.length <= LINK_URL_MAX && LINK_URL.test(value)
}

// 노출 기간은 서비스 시간대(한국, +09:00 고정)의 날짜("YYYY-MM-DD")와 시각("HH:mm")으로 받는다
const SITE_OFFSET = "+09:00"

export function toOffsetDateTime(date: string, time: string): string {
  return `${date}T${time}:00${SITE_OFFSET}`
}

export function fromOffsetDateTime(value: string | null | undefined): { date: string; time: string } {
  if (!value) return { date: "", time: "" }
  const [date = "", time = ""] = formatDateTime(value).split(" ")
  return { date, time }
}

// 목록의 기간: "2026-12-01 10:00 ~ 2026-12-10 23:59", 끝이 없으면 "2026-12-01 10:00 ~"
export function periodText(startsAt: string, endsAt?: string | null): string {
  return `${formatDateTime(startsAt)} ~ ${endsAt ? formatDateTime(endsAt) : ""}`.trim()
}

// 상태 거르기 선택지. 빈 값이 기본(끝남 빼고), ALL이 전체 (research R4)
export const STATUS_FILTERS: readonly ("" | ExposureStatusFilter)[] = [
  "",
  "SCHEDULED",
  "ACTIVE",
  "ENDED",
  "DISABLED",
  "ALL",
]
