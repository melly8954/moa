import { UserShell } from "@/components/shell/app-shell"
import { MemberOnly } from "@/components/shell/member-only"
import { SilentAuth } from "@/providers/silent-auth"

// 사용자 셸 화면 (첫 화면, 공지). 로그인 없이도 열린다 (specs/001-admin-menus FR-005, 006 FR-010)
// 로그인이 필요한 화면은 (signed-in) 묶음에 두고 AuthGate로 감싼다
// moa: 관리자 계정은 사용자 셸 화면을 열 수 없다. 열면 /admin으로 보낸다 (001 FR-005, 2026-11-22)
export default function UserLayout({ children }: { children: React.ReactNode }) {
  return (
    <SilentAuth>
      <MemberOnly>
        <UserShell>{children}</UserShell>
      </MemberOnly>
    </SilentAuth>
  )
}
