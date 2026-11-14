"use client"

import { Skeleton } from "@/components/ui/skeleton"
import { AdminShell } from "./admin-shell"
import { UserShell } from "./user-shell"
import { useMyMenus } from "./use-menu"

// 계정 화면(알림, 설정, 비밀번호 변경)의 셸: 관리 메뉴가 있으면 관리 셸, 없으면 사용자 셸 (specs/001-admin-menus FR-005, research R7)
export function AccountShell({ children }: { children: React.ReactNode }) {
  const menus = useMyMenus()
  // 메뉴를 읽지 못하면 사용자 셸로 둔다 (권한은 화면마다 API가 막는다)
  if (menus.isPending) {
    return (
      <div className="flex min-h-svh flex-col gap-3 p-(--page-padding)" aria-busy="true">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }
  return (menus.data?.length ?? 0) > 0 ? <AdminShell>{children}</AdminShell> : <UserShell>{children}</UserShell>
}
