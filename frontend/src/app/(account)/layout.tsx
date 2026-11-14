import { AccountShell } from "@/components/shell/app-shell"
import { AuthGate } from "@/providers/auth-gate"

// 계정 화면 (알림, 설정, 비밀번호 변경). 로그인을 확인하고 사용자에 맞는 셸로 감싼다 (specs/001-admin-menus FR-005)
export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGate>
      <AccountShell>{children}</AccountShell>
    </AuthGate>
  )
}
