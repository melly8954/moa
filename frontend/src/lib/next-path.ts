// 로그인 뒤 돌아갈 주소 (?next=). 로그인이 필요한 화면을 열다 로그인 화면으로 오면 AuthGate가 붙인다
// 같은 사이트 안의 경로만 받는다: "/"로 시작하고 "//"·"/\"로 시작하지 않는다 (다른 사이트로 보내는 주소 막기)

export const SIGN_IN_PATH = "/sign-in"
export const PASSWORD_PATH = "/account/password"
// 설정 (moa: 브라우저 알림 켜기·끄기, specs/007-notifications FR-008)
export const SETTINGS_PATH = "/settings"
// 마이페이지 (moa: specs/009-mypage FR-001)
export const PROFILE_PATH = "/account/profile"
// 마이페이지 계정 관리 탭 (moa: specs/018-member-withdrawal FR-001)
export const ACCOUNT_MANAGE_PATH = "/account/profile/manage"
// 탈퇴한 계정 복구 (moa: specs/018-member-withdrawal FR-010). 로그인 흐름의 화면이라 돌아갈 주소로 받지 않는다
export const RECOVER_PATH = `${SIGN_IN_PATH}/recover`

export function safeNextPath(value: string | null | undefined): string | undefined {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) return undefined
  if (value === SIGN_IN_PATH || value.startsWith(`${SIGN_IN_PATH}?`)) return undefined
  if (value === RECOVER_PATH || value.startsWith(`${RECOVER_PATH}?`)) return undefined
  return value
}

// moa: 탈퇴한 뒤의 로그인 화면. 복구 기한(완전 삭제 예정 시각)을 안내한다 (specs/018-member-withdrawal US1-AC5)
export function withdrawnSignInPath(purgeAt: string): string {
  return `${SIGN_IN_PATH}?withdrawnUntil=${encodeURIComponent(purgeAt)}`
}

// 로그인 화면 주소. next가 "/"이거나 받을 수 없는 값이면 붙이지 않는다
export function signInPath(next?: string): string {
  const safe = safeNextPath(next)
  return safe && safe !== "/" ? `${SIGN_IN_PATH}?next=${encodeURIComponent(safe)}` : SIGN_IN_PATH
}

// 지금 주소의 ?next= (브라우저에서만)
export function currentNextPath(): string | undefined {
  if (typeof window === "undefined") return undefined
  return safeNextPath(new URLSearchParams(window.location.search).get("next"))
}
