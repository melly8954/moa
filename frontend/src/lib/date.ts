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

// months달 앞(음수) 또는 뒤의 같은 날. 그 날이 없는 달이면 그 달 마지막 날 (예: 05-31의 3달 앞 → 02-28)
export function addMonths(value: DateString, months: number): DateString {
  const date = toCalendarDate(value)
  if (!date) return ""
  const day = date.getDate()
  const target = new Date(date.getFullYear(), date.getMonth() + months, 1)
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
  target.setDate(Math.min(day, lastDay))
  return fromCalendarDate(target)
}

// 기간 빠른 단추. 모두 오늘을 포함한다 (moa, specs/011-list-screen FR-010·FR-011, 2026-11-21)
//   오늘: 오늘 하루 / 7일: 오늘까지 7일 / 이번 달: 1일부터 오늘 / 3개월·1년: 3개월·1년 전 날짜의 다음 날부터 오늘
export const QUICK_RANGES = ["today", "last7Days", "thisMonth", "last3Months", "lastYear"] as const
export type QuickRange = (typeof QUICK_RANGES)[number]

export function quickRange(kind: QuickRange, now: Date = new Date()): DateRange {
  const end = today(now)
  switch (kind) {
    case "today":
      return { from: end, to: end }
    case "last7Days":
      return { from: addDays(end, -6), to: end }
    case "thisMonth":
      return { from: `${end.slice(0, 8)}01`, to: end }
    case "last3Months":
      return { from: addDays(addMonths(end, -3), 1), to: end }
    case "lastYear":
      return { from: addDays(addMonths(end, -12), 1), to: end }
  }
}

// 지금 기간과 같은 빠른 단추. 없으면 undefined (날짜를 직접 고른 경우)
export function matchingQuickRange(range: DateRange, now: Date = new Date()): QuickRange | undefined {
  return QUICK_RANGES.find((kind) => {
    const candidate = quickRange(kind, now)
    return candidate.from === range.from && candidate.to === range.to
  })
}
