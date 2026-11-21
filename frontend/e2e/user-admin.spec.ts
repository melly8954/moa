import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 사용자 관리 (user-admin 헬퍼). 백엔드 대신 API를 가로챈다. 나는 id 2 (e2e), 보안 담당 역할을 받은 운영자다
// moa (specs/002-admin-users FR-005~FR-007, research R11·R12, 2026-11-21): 계정 종류는 회원(USER)·관리자(ADMIN)
//   관리자 계정은 드로어에서 역할을 체크해 준다. 다룰 수 없는 계정(protection)은 단추가 꺼지고 이유가 보인다

type RoleRef = { id: number; name: string }
const user = (
  over: { id: number; loginId: string; name: string; accessRoles?: RoleRef[] } & Record<string, unknown>,
) => ({
  role: "ADMIN",
  locked: false,
  lockedAt: null,
  passwordChangeRequired: false,
  hasPassword: true,
  createdAt: "2026-11-01T09:00:00+09:00",
  superAdmin: false,
  protection: null,
  ...over,
  accessRoles: over.accessRoles ?? [],
})
const USERS = [
  user({ id: 2, loginId: "e2e", name: "나", accessRoles: [{ id: 1, name: "보안 담당" }] }),
  // 최고 관리자: 나는 최고 관리자가 아니라 다룰 수 없다
  user({
    id: 1,
    loginId: "admin",
    name: "관리자",
    createdAt: "2026-10-01T09:00:00+09:00",
    accessRoles: [{ id: 9, name: "최고 관리자" }],
    superAdmin: true,
    protection: "SUPER_ADMIN_ONLY",
  }),
  user({ id: 4, loginId: "lee", name: "이공지", accessRoles: [{ id: 3, name: "공지 담당" }] }),
  user({ id: 5, loginId: "norole", name: "박운영" }),
  user({
    id: 3,
    loginId: "kim",
    name: "김하늘",
    role: "USER",
    locked: true,
    lockedAt: "2026-11-05T10:00:00+09:00",
    passwordChangeRequired: true,
    hasPassword: false,
    createdAt: "2026-11-02T09:00:00+09:00",
  }),
]
// 권한 관리의 역할 목록 (specs/003-admin-roles research R18): 내가 줄 수 있는지와 이유가 붙는다
const ROLES = [
  {
    id: 9,
    code: "SUPER_ADMIN",
    name: "최고 관리자",
    description: null,
    permissions: [],
    userCount: 1,
    fixed: true,
    permissionNames: [],
    grantable: false,
    blockReason: "SUPER_ADMIN_ONLY",
  },
  {
    id: 1,
    code: "SECURITY",
    name: "보안 담당",
    description: null,
    permissions: ["LOGIN_HISTORY_READ"],
    userCount: 1,
    fixed: false,
    permissionNames: ["접속 이력 조회"],
    grantable: true,
    blockReason: null,
  },
  {
    id: 3,
    code: "NOTICES",
    name: "공지 담당",
    description: null,
    permissions: ["NOTICE_MANAGE"],
    userCount: 1,
    fixed: false,
    permissionNames: ["공지 관리"],
    grantable: false,
    blockReason: "WIDER_PERMISSIONS",
  },
]

type Sent = { roles?: unknown }

async function mock(page: Page, { roleManage = true, sent = {} as Sent } = {}) {
  await signedIn(page, {
    me: { role: "ADMIN" },
    permissions: roleManage ? ["USER_MANAGE", "ROLE_MANAGE", "LOGIN_HISTORY_READ"] : ["USER_MANAGE"],
  })
  const list = { content: USERS, page: 0, size: 20, totalElements: USERS.length, totalPages: 1 }
  await page.route("**/api/v1/admin/users?*", (route) => route.fulfill({ json: list }))
  await page.route("**/api/v1/admin/users", (route) =>
    route.request().method() === "POST"
      ? route.fulfill({
          status: 201,
          json: { user: { ...USERS[4], id: 9, loginId: "new" }, temporaryPassword: "aB3$kP9mQ2xZ" },
        })
      : route.fulfill({ json: list }),
  )
  await page.route("**/api/v1/admin/roles", (route) => route.fulfill({ json: ROLES }))
  await page.route("**/api/v1/admin/users/*/roles", (route) => {
    if (route.request().method() === "PUT") {
      sent.roles = route.request().postDataJSON()
      return route.fulfill({ status: 204 })
    }
    const id = Number(new URL(route.request().url()).pathname.split("/").at(-2))
    const found = USERS.find((u) => u.id === id)
    return route.fulfill({ json: ROLES.filter((r) => found?.accessRoles.some((a) => a.id === r.id)) })
  })
}

// moa: 행의 "관리" 메뉴 대신 아이디를 누르면 드로어가 열린다 (specs/002-admin-users FR-007)
//   사이드바의 내 계정 메뉴도 이름이 아이디라서 목록 영역 안에서 찾는다 (목록이 뜨기를 기다리는 효과도 있다)
const openUser = async (page: Page, loginId: string) => {
  await page.getByRole("region", { name: "사용자 관리" }).getByRole("button", { name: loginId, exact: true }).click()
  return page.getByRole("dialog")
}

test("사용자 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByText("총 5건")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("상태: 잠김이 먼저, 그다음 비밀번호 변경 전", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByRole("row", { name: /kim/ })).toContainText("잠김")
})

test("US3-AC1 목록의 계정 종류는 회원·관리자이고, 역할 열은 운영자의 역할·최고 관리자·역할 없음을 보인다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByRole("columnheader", { name: "계정 종류" })).toBeVisible()
  await expect(page.getByRole("row", { name: /kim/ })).toContainText("회원")
  await expect(page.getByRole("cell", { name: "보안 담당", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "최고 관리자", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "역할 없음", exact: true })).toBeVisible()
})

test("US3-AC2 내 행 드로어는 계정 종류·잠그기·삭제가 꺼져 있고 행의 관리 메뉴 단추는 없다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByRole("button", { name: /관리$/ })).toHaveCount(0)
  const drawer = await openUser(page, "e2e")
  await expect(drawer.getByRole("button", { name: "계정 종류 바꾸기" })).toBeDisabled()
  await expect(drawer.getByRole("button", { name: "잠그기" })).toBeDisabled()
  await expect(drawer.getByRole("button", { name: "삭제" })).toBeDisabled()
  await expect(drawer.getByRole("button", { name: "이름 바꾸기" })).toBeEnabled()
})

test("US3-AC2·AC3 관리자 계정 드로어에서 역할을 체크해 저장한다. 줄 수 없는 역할은 꺼진 채 이유가 보인다", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, { sent })
  await page.goto("/admin/users")
  const drawer = await openUser(page, "norole")
  await expect(drawer.getByRole("checkbox", { name: "보안 담당" })).not.toBeChecked()
  await expect(drawer.getByText("접속 이력 조회", { exact: false })).toBeVisible()
  await expect(drawer.getByRole("checkbox", { name: "최고 관리자" })).toBeDisabled()
  await expect(drawer.getByRole("checkbox", { name: "공지 담당" })).toBeDisabled()
  await expect(drawer.getByText(/내가 갖지 않은 권한이 들어 있어요/)).toBeVisible()
  await expect(drawer.getByRole("link", { name: "권한 관리" })).toHaveAttribute("href", "/admin/roles")
  await expectNoSeriousA11yViolations(page)

  await drawer.getByRole("checkbox", { name: "보안 담당" }).click()
  await drawer.getByRole("button", { name: "역할 저장" }).click()
  await expect.poll(() => sent.roles).toEqual({ roleIds: [1] })
})

test("US3-AC4 회원 드로어에는 역할 체크 칸 대신 계정 종류를 바꾸라는 안내가 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  const drawer = await openUser(page, "kim")
  await expect(
    drawer.getByText("회원은 역할을 받지 않아요. 계정 종류를 관리자로 바꾸면 역할을 줄 수 있어요."),
  ).toBeVisible()
  await expect(drawer.getByRole("button", { name: "역할 저장" })).toHaveCount(0)
})

test("US4-AC3 최고 관리자 계정은 최고 관리자만 다룰 수 있어 단추가 모두 꺼져 있고 이유가 보인다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  const drawer = await openUser(page, "admin")
  await expect(drawer.getByText("최고 관리자 계정은 최고 관리자만 다룰 수 있어요.")).toBeVisible()
  for (const name of ["이름 바꾸기", "계정 종류 바꾸기", "잠그기", "비밀번호 초기화", "삭제"])
    await expect(drawer.getByRole("button", { name })).toBeDisabled()
})

test("US4-AC4 권한 관리 권한이 없으면 역할은 이름만 보이고 체크 칸이 없다", async ({ page }) => {
  await mock(page, { roleManage: false })
  await page.goto("/admin/users")
  const drawer = await openUser(page, "lee")
  await expect(drawer.getByText("공지 담당")).toBeVisible()
  await expect(drawer.getByText("역할을 주고 빼려면 권한 관리 권한이 필요해요.")).toBeVisible()
  await expect(drawer.getByRole("checkbox")).toHaveCount(0)
})

test("US4-AC5 관리자 계정을 회원으로 바꾸려 하면 빠질 역할을 알린다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  const drawer = await openUser(page, "lee")
  await drawer.getByRole("button", { name: "계정 종류 바꾸기" }).click()
  const dialog = page.getByRole("dialog", { name: "계정 종류 바꾸기" })
  await dialog.getByLabel("계정 종류").selectOption("USER")
  await expect(dialog.getByText(/받은 역할\(공지 담당\)이 모두 빠지고/)).toBeVisible()
})

test("비밀번호 없는 계정은 초기화가 꺼져 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  const drawer = await openUser(page, "kim")
  await expect(drawer.getByRole("button", { name: "비밀번호 초기화" })).toBeDisabled()
})

test("만들기도 드로어에서 하고(계정 종류는 회원·관리자) 임시 비밀번호를 드로어 안에 한 번 보인다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await page.getByRole("button", { name: "사용자 만들기" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("아이디").fill("new")
  await dialog.getByLabel("이름").fill("새 사용자")
  await expect(dialog.getByLabel("계정 종류").locator("option")).toHaveText(["회원", "관리자"])
  await dialog.getByRole("button", { name: "저장" }).click()
  // moa: 만들기 드로어 안에 임시 비밀번호를 한 번 보인다 (specs/002-admin-users FR-007)
  await expect(dialog.getByLabel("임시 비밀번호")).toHaveValue("aB3$kP9mQ2xZ")
  await expectNoSeriousA11yViolations(page)
  await dialog.getByRole("button", { name: "확인" }).click()
  await expect(dialog).toBeHidden()
})
