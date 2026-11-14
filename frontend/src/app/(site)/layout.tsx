import { SiteHeader } from "@/components/notice/site-header"

// 로그인하지 않아도 보는 사용자 화면 (공지 등, specs/006-notices research R7)
// 셸 없이 간단한 머리만 둔다. 위치와 모양은 사용자 쪽 담당과 맞춘 뒤 바꾼다 (FR-010)
export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main className="mx-auto flex w-full max-w-3xl min-w-0 flex-1 flex-col gap-(--page-gap) p-(--page-padding)">
        {children}
      </main>
    </div>
  )
}
