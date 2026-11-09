// 쓰는 언어. 문구 파일은 messages/<언어>.json이고 ko.json이 기준이다
// 언어가 하나면 언어 전환 버튼을 숨긴다. 언어를 더하면 여기와 messages/에 파일을 더하고 npm run check:i18n으로 빠진 키를 본다
export const LOCALES = ["ko"] as const
export type Locale = (typeof LOCALES)[number]

export const DEFAULT_LOCALE: Locale = "ko"

// 고른 언어는 쿠키에 둔다 (주소에 언어를 넣지 않는다)
export const LOCALE_COOKIE = "locale"

export function isLocale(value: string | undefined): value is Locale {
  return LOCALES.includes(value as Locale)
}
