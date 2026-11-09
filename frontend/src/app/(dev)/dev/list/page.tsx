import { notFound } from "next/navigation"
import { ListDemo } from "./list-demo"

// 목록 화면 공통 모양 보기: PageHeader + SearchPanel + DataTable을 가짜 데이터로 묶었다
// 개발 서버에서만 열린다. 운영 빌드에서 열려면(화면 테스트) 실행할 때 SHOW_DEV_PAGES=true를 준다
// 프로젝트 목록 화면은 이 파일(list-demo.tsx)을 본떠 만든다. 가짜 조회 대신 api.v1.<도메인>.getPage를 부른다
export const dynamic = "force-dynamic"

export default function ListDemoPage() {
  if (process.env.NODE_ENV === "production" && process.env.SHOW_DEV_PAGES !== "true") notFound()
  return (
    <main className="flex min-h-svh flex-col p-(--page-padding)">
      <ListDemo />
    </main>
  )
}
