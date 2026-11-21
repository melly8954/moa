"use client"

import { ShieldOff } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useEffect } from "react"
import { firstMenuPath } from "@/components/shell/landing"
import { useMyMenus } from "@/components/shell/use-menu"
import { Skeleton } from "@/components/ui/skeleton"
import { useMe } from "@/lib/me"

// 관리 셸 첫 화면: 볼 수 있는 첫 관리 메뉴로 보낸다 (specs/001-admin-menus FR-005·FR-006, research R6·R27)
// moa (2026-11-21): 관리자 계정인데 볼 메뉴가 없으면(역할이 없는 운영자) 안내를 보인다. 회원이면 사용자 셸 첫 화면으로 보낸다
export default function AdminHomePage() {
  const t = useTranslations("adminHome")
  const router = useRouter()
  const me = useMe()
  const menus = useMyMenus()
  const target = menus.data ? firstMenuPath(menus.data) : undefined
  const member = me.data && me.data.role !== "ADMIN"
  useEffect(() => {
    if (member || me.isError || menus.isError) router.replace("/")
    else if (target) router.replace(target)
  }, [member, me.isError, menus.isError, target, router])

  if (me.data?.role === "ADMIN" && menus.data && !target) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 p-8 text-center">
        <ShieldOff className="size-8 text-muted-foreground" aria-hidden />
        <h1 className="text-lg font-medium">{t("noPermissionTitle")}</h1>
        <p className="text-sm text-muted-foreground">{t("noPermissionDescription")}</p>
      </div>
    )
  }
  return (
    <div className="flex flex-col gap-3" aria-busy="true">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
    </div>
  )
}
