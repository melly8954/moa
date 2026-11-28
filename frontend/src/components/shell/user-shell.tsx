"use client"

import { LegalFooter } from "@/components/legal/legal-footer"
import { SidebarShell } from "./sidebar-shell"
import { useUserMenu } from "./use-menu"

// 사용자 셸: 사이드바에 홈, 공지 (specs/001-admin-menus FR-005). 메뉴와 모양은 사용자 쪽 담당이 더하거나 바꾼다
// 본문 맨 아래에 약관·방침 푸터 줄 (specs/012-terms-privacy FR-004). 관리 셸에는 두지 않는다
export function UserShell({ children }: { children: React.ReactNode }) {
  return (
    <SidebarShell menu={useUserMenu()} homeHref="/" footer={<LegalFooter />}>
      {children}
    </SidebarShell>
  )
}
