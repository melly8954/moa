import { SITE } from "./site"

// 화면에 보이는 날짜·시각·숫자·화폐 모양. 화면마다 따로 만들지 않고 이것을 쓴다
// 브라우저·Node에 있는 Intl로 그린다. 시각은 SITE.timeZone(기본 Asia/Seoul)으로 보인다
//   → 서버 컴포넌트와 브라우저가 같은 글자를 그린다 (브라우저 시간대·언어를 따르지 않는다)
// 백엔드 시각은 ISO-8601 시간대 포함(CONTRACT.md), 날짜만 있는 값은 "YYYY-MM-DD"다
// 값이 없거나 읽지 못하면 EMPTY를 돌려준다

export const EMPTY = "-"

type DateInput = string | Date | null | undefined
type TimeZoneOption = { timeZone?: string }

const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/

function toDate(value: DateInput): Date | undefined {
  if (value === null || value === undefined || value === "") return undefined
  const date = typeof value === "string" ? new Date(value) : value
  return Number.isNaN(date.getTime()) ? undefined : date
}

// 시간대에 맞춘 연·월·일·시·분·초 조각 (Intl은 모양을 언어마다 다르게 그리므로 조각으로 받아 직접 잇는다)
function parts(date: Date, timeZone: string) {
  const formatted = new Intl.DateTimeFormat("en-US", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date)
  const get = (type: Intl.DateTimeFormatPartTypes) => formatted.find((p) => p.type === type)?.value ?? ""
  return { y: get("year"), m: get("month"), d: get("day"), h: get("hour"), min: get("minute"), s: get("second") }
}

// 2026-11-06. 날짜만 있는 값("2026-11-06")은 시간대를 거치지 않고 그대로 보인다
export function formatDate(value: DateInput, { timeZone = SITE.timeZone }: TimeZoneOption = {}): string {
  if (typeof value === "string" && DATE_ONLY.test(value)) return value
  const date = toDate(value)
  if (!date) return EMPTY
  const p = parts(date, timeZone)
  return `${p.y}-${p.m}-${p.d}`
}

// 2026-11-06 14:30 (표·목록의 시각은 연도까지 보인다). seconds: true면 14:30:05
export function formatDateTime(
  value: DateInput,
  { timeZone = SITE.timeZone, seconds = false }: TimeZoneOption & { seconds?: boolean } = {},
): string {
  const date = toDate(value)
  if (!date) return EMPTY
  const p = parts(date, timeZone)
  return `${p.y}-${p.m}-${p.d} ${p.h}:${p.min}${seconds ? `:${p.s}` : ""}`
}

// "3분 전", "2일 뒤". now는 테스트용
export function formatRelative(value: DateInput, locale = "ko", now: Date = new Date()): string {
  const date = toDate(value)
  if (!date) return EMPTY
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000)
  const units: [Intl.RelativeTimeFormatUnit, number][] = [
    ["year", 31_536_000],
    ["month", 2_592_000],
    ["day", 86_400],
    ["hour", 3_600],
    ["minute", 60],
  ]
  const format = new Intl.RelativeTimeFormat(locale, { numeric: "auto" })
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) return format.format(Math.trunc(seconds / size), unit)
  }
  return format.format(0, "minute")
}

// 1,234,567 (소수는 있는 만큼, 최대 maximumFractionDigits자리)
export function formatNumber(value: number | null | undefined, maximumFractionDigits = 2): string {
  if (value === null || value === undefined || Number.isNaN(value)) return EMPTY
  return new Intl.NumberFormat("en-US", { maximumFractionDigits }).format(value)
}

// ₩12,000 (기본 화폐 SITE.currency). 다른 화폐면 formatCurrency(10, "USD") → US$10.00
export function formatCurrency(value: number | null | undefined, currency = SITE.currency): string {
  if (value === null || value === undefined || Number.isNaN(value)) return EMPTY
  return new Intl.NumberFormat("ko-KR", { style: "currency", currency }).format(value)
}

// 1.5 MB (파일 크기)
export function formatBytes(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return EMPTY
  const units = ["B", "KB", "MB", "GB", "TB"]
  let size = value
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit++
  }
  return `${formatNumber(size, unit === 0 ? 0 : 1)} ${units[unit]}`
}
