"use client"

import { Languages } from "lucide-react"
import { useRouter } from "next/navigation"
import { useLocale, useTranslations } from "next-intl"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { LOCALE_COOKIE, LOCALES } from "@/i18n/config"

// 언어가 둘 이상일 때만 언어 고르기를 보인다 (i18n/config.ts LOCALES)
export const HAS_LOCALE_CHOICE = LOCALES.length > 1

// 언어 고르기. 셸은 HAS_LOCALE_CHOICE일 때만 둔다
// 고른 언어는 쿠키에 1년 두고, 화면을 서버에서 다시 그린다
export function LocaleMenu({ trigger, showLabel = false }: { trigger: React.ReactElement; showLabel?: boolean }) {
  const t = useTranslations()
  const locale = useLocale()
  const router = useRouter()

  const change = (value: string) => {
    document.cookie = `${LOCALE_COOKIE}=${value}; path=/; max-age=31536000; samesite=lax`
    router.refresh()
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={trigger}>
        <Languages />
        <span className={showLabel ? undefined : "sr-only"}>{t("shell.language")}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <DropdownMenuLabel>{t("shell.language")}</DropdownMenuLabel>
          <DropdownMenuRadioGroup value={locale} onValueChange={(value) => change(value as string)}>
            {LOCALES.map((value) => (
              <DropdownMenuRadioItem key={value} value={value}>
                {t(`locales.${value}`)}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
