// 로그인하지 않아도 보는 화면 (로그인 등). 셸 없이 내용만 화면 가운데에 둔다
export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return <main className="flex min-h-svh flex-col items-center justify-center p-4">{children}</main>
}
