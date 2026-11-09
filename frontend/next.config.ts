import type { NextConfig } from "next"
import createNextIntlPlugin from "next-intl/plugin"

// 백엔드 프록시는 src/proxy.ts에 있다 (주소를 실행할 때 읽는다)
// 문구(next-intl)는 src/i18n/request.ts가 요청마다 고른다
const nextConfig: NextConfig = {}

export default createNextIntlPlugin("./src/i18n/request.ts")(nextConfig)
