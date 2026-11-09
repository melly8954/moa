import { type NextRequest, NextResponse } from "next/server"

// 백엔드 프록시: 브라우저가 부른 /api/* 를 같은 경로 그대로 백엔드(API_BASE_URL)로 넘긴다
//   → 브라우저에는 같은 출처라 CORS가 필요 없고, 리프레시 토큰 쿠키(경로 /api/v1/auth)가 그대로 붙는다
// next.config.ts의 rewrites는 주소가 빌드할 때 굳는다. 여기서는 실행할 때 읽으므로 빌드 하나를 환경마다 쓸 수 있다
// 경로를 바꿔 넘기면 쿠키 경로가 맞지 않는다. 프록시 앞뒤 경로를 같게 둔다
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl
  return NextResponse.rewrite(new URL(pathname + search, process.env.API_BASE_URL ?? "http://localhost:8080"))
}

export const config = { matcher: "/api/:path*" }
