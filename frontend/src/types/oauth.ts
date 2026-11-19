// moa: 소셜 로그인·약관 API의 응답 (specs/010-social-login contracts/auth-oauth.md)
// 타입은 npm run gen:api 뒤 생성 타입으로 바꾼다 (tasks T052)

// GET /api/v1/auth/oauth/providers. 설정된 제공자의 경로 값 (예: "google")
export type OAuthProviders = {
  providers: string[]
}

export type TermsType = "SERVICE" | "PRIVACY"

// GET /api/v1/terms. 가입 때 동의받을 현재 약관
export type CurrentTerms = {
  terms: { type: TermsType; version: string; required: boolean }[]
}
