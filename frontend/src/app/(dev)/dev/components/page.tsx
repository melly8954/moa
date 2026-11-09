import { notFound } from "next/navigation"
import { Gallery } from "./gallery"

// 전체 부품 확인 화면. 개발 서버에서만 열린다
// 운영 빌드에서 열려면(화면 테스트) 실행할 때 SHOW_DEV_PAGES=true를 준다
export const dynamic = "force-dynamic"

export default function ComponentsPage() {
  if (process.env.NODE_ENV === "production" && process.env.SHOW_DEV_PAGES !== "true") notFound()
  return <Gallery />
}
