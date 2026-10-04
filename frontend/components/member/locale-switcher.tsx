"use client"

import { useLocale, useTranslations } from "next-intl"
import { useRouter } from "next/navigation"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"

const LOCALE_COOKIE = "NEXT_LOCALE"
const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365
const LOCALES = ["ko", "en"] as const

/** 비회원 화면 아래의 언어 고르기. 선택지가 둘이라 RadioGroup. 쿠키에 두고 화면을 다시 그린다 (NFR-I18N-001) */
export function LocaleSwitcher() {
  const t = useTranslations()
  const locale = useLocale()
  const router = useRouter()

  function change(next: unknown) {
    if (typeof next !== "string" || next === locale) {
      return
    }
    document.cookie = `${LOCALE_COOKIE}=${next}; path=/; max-age=${ONE_YEAR_SECONDS}; samesite=lax`
    router.refresh()
  }

  return (
    <RadioGroup
      value={locale}
      onValueChange={change}
      aria-label={t("common.language")}
      className="flex w-auto justify-center gap-4"
    >
      {LOCALES.map((value) => (
        <div key={value} className="flex items-center gap-2">
          <RadioGroupItem value={value} id={`locale-${value}`} />
          <Label htmlFor={`locale-${value}`}>{t(`locale.${value}`)}</Label>
        </div>
      ))}
    </RadioGroup>
  )
}
