"use client"

import { KeyRound, LogOut, Settings, SlidersHorizontal, UserRound } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { NotificationBell } from "@/components/notification/notification-bell"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useSidebar } from "@/components/ui/sidebar"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { useMe } from "@/lib/me"
import { PASSWORD_PATH, SETTINGS_PATH } from "@/lib/next-path"
import { cn } from "@/lib/utils"
import { HAS_LOCALE_CHOICE, LocaleChoices } from "./locale-menu"
import { ThemeChoices } from "./theme-menu"
import { useSignOut } from "./use-sign-out"

// 사이드바 바닥의 계정 줄 (moa, specs/001-admin-menus FR-005, 2026-11-21. 키트로 옮길 것)
//   한 줄에 누구인지(이름·아이디)와 아이콘 단추 셋: 알림 벨(안 읽은 수), 톱니, 로그아웃
//   톱니 메뉴: 화면 모드, (언어가 여럿이면) 언어, 설정, 비밀번호 변경. 로그아웃은 묻지 않고 바로 한다
//   아이콘만 남게 접힌 사이드바에서는 이름을 숨기고 세 단추를 세로로 쌓는다. 단추 이름은 툴팁으로 보인다
export function AccountBar() {
  const t = useTranslations("shell")
  const me = useMe().data
  const signOut = useSignOut()
  const { state, isMobile } = useSidebar()
  const collapsed = state === "collapsed" && !isMobile
  const side = collapsed ? "right" : "top"

  return (
    <div className={cn("flex items-center gap-1", collapsed ? "flex-col" : "px-1 py-0.5")}>
      {!collapsed && (
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-sidebar-accent">
            <UserRound aria-hidden="true" className="size-4" />
          </span>
          <span className="flex min-w-0 flex-col leading-tight">
            <span className="truncate text-sm font-medium">{me?.name ?? t("account")}</span>
            <span className="truncate text-xs text-muted-foreground">{me?.loginId}</span>
          </span>
        </div>
      )}
      {/* 알림 벨: 단추 이름(안 읽은 수 포함)은 벨이 정한다 */}
      <Tooltip>
        <NotificationBell trigger={<TooltipTrigger render={<Button variant="ghost" size="icon-sm" />} />} />
        <TooltipContent side={side}>{t("notifications")}</TooltipContent>
      </Tooltip>
      <DropdownMenu>
        <Tooltip>
          <DropdownMenuTrigger
            render={<TooltipTrigger render={<Button variant="ghost" size="icon-sm" />} />}
            aria-label={t("preferences")}
          >
            <Settings aria-hidden="true" />
          </DropdownMenuTrigger>
          <TooltipContent side={side}>{t("preferences")}</TooltipContent>
        </Tooltip>
        <DropdownMenuContent align="end" side={side} className="min-w-48">
          <ThemeChoices />
          {HAS_LOCALE_CHOICE && (
            <>
              <DropdownMenuSeparator />
              <LocaleChoices />
            </>
          )}
          <DropdownMenuSeparator />
          <DropdownMenuItem render={<Link href={SETTINGS_PATH} />}>
            <SlidersHorizontal />
            {t("settings")}
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href={PASSWORD_PATH} />}>
            <KeyRound />
            {t("changePassword")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <Tooltip>
        <TooltipTrigger
          render={<Button variant="ghost" size="icon-sm" />}
          aria-label={t("signOut")}
          onClick={() => void signOut()}
        >
          <LogOut aria-hidden="true" />
        </TooltipTrigger>
        <TooltipContent side={side}>{t("signOut")}</TooltipContent>
      </Tooltip>
    </div>
  )
}
