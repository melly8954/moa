import { formatDate } from "./format"

// 날짜 고르기 값은 언제나 "YYYY-MM-DD" 문자열이다 (빈 값은 "")
//   Date로 들고 다니면 toISOString()이 UTC로 바꾸면서 하루 앞당겨지는 일이 생긴다
//   달력(react-day-picker)과 주고받을 때만 그 날 0시의 브라우저 Date로 바꾼다
// "오늘"은 SITE.timeZone(서울) 기준이다

export type DateString = string
export type DateRange = { from: DateString; to: DateString }

const PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/

// 있는 날짜인 "YYYY-MM-DD"인가 (2026-02-30은 아니다)
export function isDateString(value: string): boolean {
  const match = PATTERN.exec(value)
  if (!match) return false
  const [y, m, d] = match.slice(1).map(Number)
  const date = new Date(y, m - 1, d)
  return date.getFullYear() === y && date.getMonth() === m - 1 && date.getDate() === d
}

// 달력에 넘길 Date (그 날 0시, 브라우저 시간). 아니면 undefined
export function toCalendarDate(value: DateString): Date | undefined {
  if (!isDateString(value)) return undefined
  const [y, m, d] = value.split("-").map(Number)
  return new Date(y, m - 1, d)
}

// 달력이 준 Date → "YYYY-MM-DD" (브라우저 시간의 연·월·일 그대로. toISOString을 쓰지 않는다)
export function fromCalendarDate(date: Date | undefined): DateString {
  if (!date) return ""
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// 입력칸에 친 글자를 날짜로 읽는다. "20261106", "2026.11.6", "2026/11/06"도 받는다. 못 읽으면 undefined
export function parseTypedDate(text: string): DateString | undefined {
  const trimmed = text.trim()
  if (trimmed === "") return ""
  const digits = /^\d{8}$/.test(trimmed)
    ? [trimmed.slice(0, 4), trimmed.slice(4, 6), trimmed.slice(6, 8)]
    : trimmed.split(/[-./\s]+/)
  if (digits.length !== 3) return undefined
  const [y, m, d] = digits
  const value = `${y.padStart(4, "0")}-${m.padStart(2, "0")}-${d.padStart(2, "0")}`
  return isDateString(value) ? value : undefined
}

export function today(now: Date = new Date()): DateString {
  return formatDate(now)
}

// days일 뒤(음수면 앞)
export function addDays(value: DateString, days: number): DateString {
  const date = toCalendarDate(value)
  if (!date) return ""
  date.setDate(date.getDate() + days)
  return fromCalendarDate(date)
}

// min~max 안인가 (빈 min·max는 끝이 없다). 문자열 비교로 된다 (YYYY-MM-DD)
export function inRange(value: DateString, min?: DateString, max?: DateString): boolean {
  return (!min || value >= min) && (!max || value <= max)
}

// 기간 빠른 선택. 끝은 모두 오늘(이번 달·지난달은 그 달 안)
export const QUICK_RANGES = ["today", "last7Days", "last30Days", "thisMonth", "lastMonth"] as const
export type QuickRange = (typeof QUICK_RANGES)[number]

export function quickRange(kind: QuickRange, now: Date = new Date()): DateRange {
  const end = today(now)
  const [y, m] = end.split("-").map(Number)
  const firstOfMonth = (year: number, month: number) => fromCalendarDate(new Date(year, month - 1, 1))
  switch (kind) {
    case "today":
      return { from: end, to: end }
    case "last7Days":
      return { from: addDays(end, -6), to: end }
    case "last30Days":
      return { from: addDays(end, -29), to: end }
    case "thisMonth":
      return { from: firstOfMonth(y, m), to: end }
    case "lastMonth":
      return { from: firstOfMonth(y, m - 1), to: addDays(firstOfMonth(y, m), -1) }
  }
}
