import type { Page } from "@playwright/test"

export const E2E_ME = {
  id: 2,
  loginId: "e2e",
  name: "e2e",
  role: "USER",
  passwordChangeRequired: false,
  hasPassword: true,
  avatar: "default",
  mypageAllowed: true,
}

// 사용자 셸 메뉴 (/menus/user, 메뉴 관리의 사용자 메뉴 처음 구성. moa: specs/001-admin-menus FR-007)
export const USER_MENUS = [
  { id: 101, name: "홈", path: "/", icon: "home", children: [] },
  { id: 102, name: "공지", path: "/notices", icon: "megaphone", children: [] },
]

// 백엔드 없이 로그인한 상태로 화면을 연다: 재발급 요청에 토큰을, 내 정보·내 권한에 정해 둔 값을 돌려준다
// 화면이 부르는 다른 API도 page.route로 가로채 첫 화면 상태를 정한다
// menus: 내 관리 메뉴(/menus/me). me.role을 주지 않으면 메뉴가 있을 때 관리자 계정(ADMIN), 없을 때 회원(USER)이다
//   moa (specs/001-admin-menus FR-005, research R28): 셸은 메뉴가 아니라 계정 종류(me.role)로 고른다.
//   역할 없는 관리자 계정은 me: { role: "ADMIN" }과 빈 menus로 연다
//   나중에 page.route로 다시 가로채면 그것이 먼저다
export async function signedIn(
  page: Page,
  {
    me = {},
    permissions = [],
    menus = [],
  }: { me?: Partial<typeof E2E_ME>; permissions?: string[]; menus?: unknown[] } = {},
) {
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: menus }))
  await page.route("**/api/v1/menus/user", (route) => route.fulfill({ json: USER_MENUS }))
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  const role = me.role ?? (menus.length > 0 ? "ADMIN" : "USER")
  await page.route("**/api/v1/users/me", (route) => route.fulfill({ json: { ...E2E_ME, role, ...me } }))
  await page.route("**/api/v1/users/me/permissions", (route) => route.fulfill({ json: { permissions } }))
}

// 로그인하지 않은 상태: 재발급이 A003으로 실패한다
export async function signedOut(page: Page) {
  await page.route("**/api/v1/menus/user", (route) => route.fulfill({ json: USER_MENUS }))
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ status: 401, json: { code: "A003", message: "", timestamp: "", path: "" } }),
  )
}
