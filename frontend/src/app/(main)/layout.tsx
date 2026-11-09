import { AppShell } from "@/components/shell/app-shell"
import { AuthGate } from "@/providers/auth-gate"

// 로그인한 사용자가 보는 화면. 로그인을 확인하고 셸로 감싼다
export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGate>
      <AppShell>{children}</AppShell>
    </AuthGate>
  )
}
