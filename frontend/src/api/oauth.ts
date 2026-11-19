import type { CurrentTerms, OAuthProviders } from "@/types/oauth"
import { http } from "./client"

// moa: 소셜 로그인 (specs/010-social-login). api.v1.oauth로 부른다. 모두 로그인 없이 부른다
//   providers: 로그인 화면에 보일 제공자 단추
// 인가 시작·콜백은 브라우저 이동이라 여기 두지 않는다 (/api/v1/auth/oauth/{provider}/authorize)
export const oauthApi = {
  providers: () => http.get<OAuthProviders>("/api/v1/auth/oauth/providers", { skipAuth: true }).then((res) => res.data),
}

// moa: 현재 약관 버전 (specs/010-social-login FR-013). api.v1.terms로 부른다
export const termsApi = {
  current: () => http.get<CurrentTerms>("/api/v1/terms", { skipAuth: true }).then((res) => res.data),
}
