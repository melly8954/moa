import { notFound } from "next/navigation"
import { RichTextDemo } from "./rich-text-demo"

// 서식 있는 글 편집기·보기 (rich-text 헬퍼). 폼에 편집기를 잇는 모양의 본보기다
// 개발 서버에서만 열린다. 운영 빌드에서 열려면(화면 테스트) 실행할 때 SHOW_DEV_PAGES=true를 준다
// 이미지 올리기는 로그인한 사용자만 된다. 이 화면은 로그인 확인 바깥이라 백엔드에 붙이면 A001이 날 수 있다
export const dynamic = "force-dynamic"

export default function RichTextDemoPage() {
  if (process.env.NODE_ENV === "production" && process.env.SHOW_DEV_PAGES !== "true") notFound()
  return (
    <main className="flex min-h-svh min-w-0 flex-col gap-(--page-gap) p-(--page-padding)">
      <RichTextDemo />
    </main>
  )
}
