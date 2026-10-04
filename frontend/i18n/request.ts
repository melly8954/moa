// 언어는 주소가 아니라 쿠키(NEXT_LOCALE)로 고른다. 기본은 한국어 (docs/design/conventions.md 3.6)
import { cookies } from "next/headers"
import { getRequestConfig } from "next-intl/server"

export const LOCALES = ["ko", "en"] as const
export type Locale = (typeof LOCALES)[number]
export const DEFAULT_LOCALE: Locale = "ko"
export const LOCALE_COOKIE = "NEXT_LOCALE"

export default getRequestConfig(async () => {
  const value = (await cookies()).get(LOCALE_COOKIE)?.value
  const locale: Locale = LOCALES.find((candidate) => candidate === value) ?? DEFAULT_LOCALE
  return {
    locale,
    timeZone: "Asia/Seoul",
    messages: (await import(`../messages/${locale}.json`)).default,
  }
})
