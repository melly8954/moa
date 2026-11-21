"use client"

import { Skeleton } from "@/components/ui/skeleton"
import { useMe } from "@/lib/me"
import { AdminShell } from "./admin-shell"
import { UserShell } from "./user-shell"

// 계정 화면(알림, 설정, 비밀번호 변경)의 셸 (specs/001-admin-menus FR-005, research R7·R27)
// moa (2026-11-21): 관리자 계정(ADMIN)이면 관리 셸, 회원이면 사용자 셸. 예전에는 관리 메뉴가 있는지로 골랐다
export function AccountShell({ children }: { children: React.ReactNode }) {
  const me = useMe()
  // 내 정보를 읽지 못하면 사용자 셸로 둔다 (권한은 화면마다 API가 막는다)
  if (me.isPending) {
    return (
      <div className="flex min-h-svh flex-col gap-3 p-(--page-padding)" aria-busy="true">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }
  return me.data?.role === "ADMIN" ? <AdminShell>{children}</AdminShell> : <UserShell>{children}</UserShell>
}
