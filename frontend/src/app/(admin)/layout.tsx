import { AdminShell } from "@/components/shell/app-shell"
import { AuthGate } from "@/providers/auth-gate"

// 관리 화면 (/admin/...). 로그인을 확인하고 관리 셸로 감싼다 (specs/001-admin-menus FR-005)
// 권한은 화면마다 API가 막는다 (A005면 목록 자리에 권한 없음 안내)
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGate>
      <AdminShell>{children}</AdminShell>
    </AuthGate>
  )
}
