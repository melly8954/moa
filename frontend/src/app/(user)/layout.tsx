import { UserShell } from "@/components/shell/app-shell"
import { SilentAuth } from "@/providers/silent-auth"

// 사용자 셸 화면 (첫 화면, 공지). 로그인 없이도 열린다 (specs/001-admin-menus FR-005, 006 FR-010)
// 로그인이 필요한 화면은 (signed-in) 묶음에 두고 AuthGate로 감싼다
export default function UserLayout({ children }: { children: React.ReactNode }) {
  return (
    <SilentAuth>
      <UserShell>{children}</UserShell>
    </SilentAuth>
  )
}
