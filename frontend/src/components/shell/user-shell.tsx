"use client"

import { SidebarShell } from "./sidebar-shell"
import { useUserMenu } from "./use-menu"

// 사용자 셸: 사이드바에 홈, 공지 (specs/001-admin-menus FR-005). 메뉴와 모양은 사용자 쪽 담당이 더하거나 바꾼다
export function UserShell({ children }: { children: React.ReactNode }) {
  return (
    <SidebarShell menu={useUserMenu()} homeHref="/">
      {children}
    </SidebarShell>
  )
}
