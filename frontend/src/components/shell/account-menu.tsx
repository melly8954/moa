"use client"

import { KeyRound, LogOut, UserRound } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useMe } from "@/lib/me"
import { PASSWORD_PATH } from "@/lib/next-path"
import { useSignOut } from "./use-sign-out"

// 내 계정 메뉴: 이름·아이디, 비밀번호 변경, 로그아웃
// trigger: 셸마다 다른 버튼 모양. 버튼 안에는 사람 아이콘과 이름이 들어간다
export function AccountMenu({ trigger, showName = true }: { trigger: React.ReactElement; showName?: boolean }) {
  const t = useTranslations("shell")
  const me = useMe().data
  const signOut = useSignOut()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={trigger}>
        <UserRound />
        <span className={showName ? "truncate" : "sr-only"}>{me?.name ?? t("account")}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-48">
        <DropdownMenuGroup>
          <DropdownMenuLabel>
            <span className="block truncate text-foreground">{me?.name}</span>
            <span className="block truncate font-normal">{me?.loginId}</span>
          </DropdownMenuLabel>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuItem render={<Link href={PASSWORD_PATH} />}>
          <KeyRound />
          {t("changePassword")}
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => void signOut()}>
          <LogOut />
          {t("signOut")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
