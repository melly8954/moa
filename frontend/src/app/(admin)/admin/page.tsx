"use client"

import { useRouter } from "next/navigation"
import { useEffect } from "react"
import { firstMenuPath } from "@/components/shell/landing"
import { useMyMenus } from "@/components/shell/use-menu"
import { Skeleton } from "@/components/ui/skeleton"

// 관리 셸 첫 화면: 볼 수 있는 첫 관리 메뉴로 보낸다. 없으면 사용자 셸 첫 화면 (specs/001-admin-menus FR-006, research R6)
export default function AdminHomePage() {
  const router = useRouter()
  const menus = useMyMenus()
  useEffect(() => {
    if (menus.data) router.replace(firstMenuPath(menus.data) ?? "/")
    else if (menus.isError) router.replace("/")
  }, [menus.data, menus.isError, router])
  return (
    <div className="flex flex-col gap-3" aria-busy="true">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
    </div>
  )
}
