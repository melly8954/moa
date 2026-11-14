"use client"

import { ADMIN_HOME } from "./landing"
import { SidebarShell } from "./sidebar-shell"
import { useAdminMenu } from "./use-menu"

// 관리 셸: 사이드바에 관리 메뉴만 둔다. 로고는 첫 관리 메뉴로 보내는 /admin (specs/001-admin-menus FR-001·FR-005, research R6)
export function AdminShell({ children }: { children: React.ReactNode }) {
  return (
    <SidebarShell menu={useAdminMenu()} homeHref={ADMIN_HOME}>
      {children}
    </SidebarShell>
  )
}
