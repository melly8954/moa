import { AuthGate } from "@/providers/auth-gate"

// 사용자 셸 안에서 로그인이 필요한 화면
export default function SignedInLayout({ children }: { children: React.ReactNode }) {
  return <AuthGate>{children}</AuthGate>
}
