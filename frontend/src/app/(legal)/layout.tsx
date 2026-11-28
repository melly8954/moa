import { LegalFooter } from "@/components/legal/legal-footer"
import { LegalHeader } from "@/components/legal/legal-header"

// 이용약관·개인정보처리방침 화면 (specs/012-terms-privacy FR-001, FR-011, research R7)
//   셸·로그인 확인 없이 누구나 연다. 관리자 계정도 막지 않는다 (사용자 셸은 관리자 계정을 /admin으로 보낸다)
//   머리 줄(moa, 두 문서 링크), 읽기 좋은 폭의 본문, 맨 아래 푸터 줄
export default function LegalLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col">
      <LegalHeader />
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-6">{children}</main>
      <LegalFooter />
    </div>
  )
}
