import { cookies } from "next/headers"
import { getRequestConfig } from "next-intl/server"
import ko from "../../messages/ko.json"
import { DEFAULT_LOCALE, isLocale, LOCALE_COOKIE } from "./config"

type Messages = Record<string, unknown>

// 빠진 번역은 기준 언어(ko) 문구로 채운다
function withFallback(base: Messages, override: Messages): Messages {
  const merged: Messages = { ...base }
  for (const [key, value] of Object.entries(override)) {
    const baseValue = base[key]
    merged[key] =
      typeof value === "object" && value !== null && typeof baseValue === "object" && baseValue !== null
        ? withFallback(baseValue as Messages, value as Messages)
        : value
  }
  return merged
}

// 요청마다 쿠키에서 언어를 읽어 문구를 고른다. 서버 데이터(코드 이름, 메뉴 이름 등)는 번역하지 않는다
export default getRequestConfig(async () => {
  const cookieLocale = (await cookies()).get(LOCALE_COOKIE)?.value
  const locale = isLocale(cookieLocale) ? cookieLocale : DEFAULT_LOCALE
  if (locale === DEFAULT_LOCALE) return { locale, messages: ko }
  const messages = (await import(`../../messages/${locale}.json`)).default as Messages
  return { locale, messages: withFallback(ko, messages) as typeof ko }
})
