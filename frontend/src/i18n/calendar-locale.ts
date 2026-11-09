import type { Locale as DayPickerLocale } from "react-day-picker"
import { ko } from "react-day-picker/locale"
import type { Locale } from "./config"

// 달력(react-day-picker)의 요일·달 이름 언어. LOCALES에 언어를 더하면 여기에도 더한다 (빠지면 typecheck 실패)
export const CALENDAR_LOCALES: Record<Locale, DayPickerLocale> = { ko }
