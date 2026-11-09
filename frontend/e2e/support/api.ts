import type { Page } from "@playwright/test"

export const E2E_ME = {
  id: 2,
  loginId: "e2e",
  name: "e2e",
  role: "USER",
  passwordChangeRequired: false,
}

// 백엔드 없이 로그인한 상태로 화면을 연다: 재발급 요청에 토큰을, 내 정보·내 권한에 정해 둔 값을 돌려준다
// 화면이 부르는 다른 API도 page.route로 가로채 첫 화면 상태를 정한다
export async function signedIn(
  page: Page,
  { me = {}, permissions = [] }: { me?: Partial<typeof E2E_ME>; permissions?: string[] } = {},
) {
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  await page.route("**/api/v1/users/me", (route) => route.fulfill({ json: { ...E2E_ME, ...me } }))
  await page.route("**/api/v1/users/me/permissions", (route) => route.fulfill({ json: { permissions } }))
}

// 로그인하지 않은 상태: 재발급이 A003으로 실패한다
export async function signedOut(page: Page) {
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ status: 401, json: { code: "A003", message: "", timestamp: "", path: "" } }),
  )
}
