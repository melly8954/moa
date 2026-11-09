// 로그인 뒤 돌아갈 주소 (?next=). 로그인이 필요한 화면을 열다 로그인 화면으로 오면 AuthGate가 붙인다
// 같은 사이트 안의 경로만 받는다: "/"로 시작하고 "//"·"/\"로 시작하지 않는다 (다른 사이트로 보내는 주소 막기)

export const SIGN_IN_PATH = "/sign-in"
export const PASSWORD_PATH = "/account/password"

export function safeNextPath(value: string | null | undefined): string | undefined {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) return undefined
  if (value === SIGN_IN_PATH || value.startsWith(`${SIGN_IN_PATH}?`)) return undefined
  return value
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
