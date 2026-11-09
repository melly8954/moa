"use client"

import { Moon, Sun } from "lucide-react"
import { useTranslations } from "next-intl"
import { useTheme } from "next-themes"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const THEMES = ["light", "dark", "system"] as const
const LABELS = { light: "themeLight", dark: "themeDark", system: "themeSystem" } as const

// 화면 모드(밝게·어둡게·기기 설정) 고르기. 고른 값은 next-themes가 브라우저에 기억한다
// trigger: 셸마다 다른 버튼 모양 (사이드바 메뉴 버튼, 헤더 아이콘 버튼)
export function ThemeMenu({ trigger, showLabel = false }: { trigger: React.ReactElement; showLabel?: boolean }) {
  const t = useTranslations("shell")
  const { theme = "system", setTheme } = useTheme()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={trigger}>
        {/* 아이콘은 CSS로 고른다: 첫 그림(서버)에는 고른 모드를 모른다 */}
        <Sun className="dark:hidden" />
        <Moon className="hidden dark:block" />
        <span className={showLabel ? undefined : "sr-only"}>{t("theme")}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <DropdownMenuLabel>{t("theme")}</DropdownMenuLabel>
          <DropdownMenuRadioGroup value={theme} onValueChange={(value) => setTheme(value as string)}>
            {THEMES.map((value) => (
              <DropdownMenuRadioItem key={value} value={value}>
                {t(LABELS[value])}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
