// 구글·카카오 인증 주소. 브라우저가 이 주소로 이동한다 (fetch가 아니다)
import { API_URL } from "./client"

export type OAuthProvider = "google" | "kakao"

/** 가입 화면에서 시작한 소셜 인증. 실패하면 서버가 /signup?error=<코드>로 돌려보낸다 */
export function oauthSignUpUrl(provider: OAuthProvider) {
  return `${API_URL}/api/v1/auth/oauth/${provider}?intent=SIGN_UP`
}
