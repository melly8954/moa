"use client"

import { useLocale, useTranslations } from "next-intl"
import { useRouter } from "next/navigation"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

const LOCALE_COOKIE = "NEXT_LOCALE"
const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365

/** 비회원 화면 아래의 언어 고르기. 쿠키에 두고 화면을 다시 그린다 (NFR-I18N-001) */
export function LocaleSwitcher() {
  const t = useTranslations()
  const locale = useLocale()
  const router = useRouter()
  const items = [
    { value: "ko", label: t("locale.ko") },
    { value: "en", label: t("locale.en") },
  ]

  function change(next: string | null) {
    if (!next || next === locale) {
      return
    }
    document.cookie = `${LOCALE_COOKIE}=${next}; path=/; max-age=${ONE_YEAR_SECONDS}; samesite=lax`
    router.refresh()
  }

  return (
    <Select items={items} value={locale} onValueChange={change}>
      <SelectTrigger size="sm" aria-label={t("common.language")}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {items.map((item) => (
          <SelectItem key={item.value} value={item.value}>
            {item.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
