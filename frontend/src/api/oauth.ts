import type { TokenResponse } from "@/types/api"
import type { CurrentTerms, OAuthProviders, OAuthSignUpRequest } from "@/types/oauth"
import { http } from "./client"

// moa: 소셜 로그인 (specs/010-social-login). api.v1.oauth로 부른다. 모두 로그인 없이 부른다
//   providers: 로그인 화면에 보일 제공자 단추
//   signUp: 닉네임·약관 동의로 가입 (가입 대기 쿠키 oauth_signup이 붙는다). 응답은 로그인과 같다
//   authorizePath: 인가 시작 주소. 브라우저 이동(window.location)으로 연다
export const oauthApi = {
  providers: () => http.get<OAuthProviders>("/api/v1/auth/oauth/providers", { skipAuth: true }).then((res) => res.data),
  signUp: (data: OAuthSignUpRequest) =>
    http.post<TokenResponse>("/api/v1/auth/oauth/sign-up", data, { skipAuth: true }).then((res) => res.data),
  authorizePath: (provider: string, next?: string) =>
    `/api/v1/auth/oauth/${encodeURIComponent(provider)}/authorize` + (next ? `?next=${encodeURIComponent(next)}` : ""),
}

// moa: 현재 약관 버전 (specs/010-social-login FR-013). api.v1.terms로 부른다
export const termsApi = {
  current: () => http.get<CurrentTerms>("/api/v1/terms", { skipAuth: true }).then((res) => res.data),
}
