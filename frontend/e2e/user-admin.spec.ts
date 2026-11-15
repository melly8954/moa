import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 사용자 관리 (user-admin 헬퍼). 백엔드 대신 API를 가로챈다. 나는 id 2 (e2e)

const USERS = [
  {
    id: 2,
    loginId: "e2e",
    name: "나",
    role: "USER",
    locked: false,
    lockedAt: null,
    passwordChangeRequired: false,
    hasPassword: true,
    createdAt: "2026-11-01T09:00:00+09:00",
    accessRoles: [{ id: 1, name: "보안 담당" }],
  },
  {
    id: 1,
    loginId: "admin",
    name: "관리자",
    role: "ADMIN",
    locked: false,
    lockedAt: null,
    passwordChangeRequired: false,
    hasPassword: true,
    createdAt: "2026-10-01T09:00:00+09:00",
    accessRoles: [],
  },
  {
    id: 3,
    loginId: "kim",
    name: "김하늘",
    role: "USER",
    locked: true,
    lockedAt: "2026-11-05T10:00:00+09:00",
    passwordChangeRequired: true,
    hasPassword: false,
    createdAt: "2026-11-02T09:00:00+09:00",
    accessRoles: [],
  },
]

async function mock(page: Page, role: "USER" | "ADMIN" = "USER") {
  await signedIn(page, { me: { role }, permissions: ["USER_MANAGE"] })
  await page.route("**/api/v1/admin/users?*", (route) =>
    route.fulfill({ json: { content: USERS, page: 0, size: 20, totalElements: 3, totalPages: 1 } }),
  )
  await page.route("**/api/v1/admin/users", (route) =>
    route.request().method() === "POST"
      ? route.fulfill({
          status: 201,
          json: { user: { ...USERS[2], id: 9, loginId: "new" }, temporaryPassword: "aB3$kP9mQ2xZ" },
        })
      : route.fulfill({ json: { content: USERS, page: 0, size: 20, totalElements: 3, totalPages: 1 } }),
  )
}

// moa: 행의 "관리" 메뉴 대신 아이디를 누르면 드로어가 열린다 (specs/002-admin-users FR-007)
const openUser = async (page: Page, loginId: string) => {
  await page.getByRole("button", { name: loginId, exact: true }).click()
  return page.getByRole("dialog")
}

test("사용자 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByText("총 3건")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("상태: 잠김이 먼저, 그다음 비밀번호 변경 전", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  await expect(page.getByRole("row", { name: /kim/ })).toContainText("잠김")
})

test("US3-AC2 내 행 드로어는 역할·잠그기·삭제가 꺼져 있고 행의 관리 메뉴 단추는 없다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await expect(page.getByRole("button", { name: /관리$/ })).toHaveCount(0)
  const drawer = await openUser(page, "e2e")
  await expect(drawer.getByRole("button", { name: "잠그기" })).toBeDisabled()
  await expect(drawer.getByRole("button", { name: "삭제" })).toBeDisabled()
  await expect(drawer.getByRole("button", { name: "이름 바꾸기" })).toBeEnabled()
  // moa: 업무 역할은 역할 관리에서만 준다 (specs/002-admin-users FR-005)
  await expect(drawer.getByRole("button", { name: "업무 역할" })).toHaveCount(0)
})

test("US3-AC1·AC3 목록에 업무 역할 열이 있고, 드로어에서는 보기만 하며 역할 관리로 가는 링크가 있다 @wide", async ({
  page,
}) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await expect(page.getByRole("cell", { name: "보안 담당", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "모든 권한", exact: true })).toBeVisible()
  const drawer = await openUser(page, "e2e")
  await expect(drawer.getByText("보안 담당")).toBeVisible()
  await expect(drawer.getByRole("link", { name: "역할 관리" })).toHaveAttribute("href", "/admin/roles")
  await expectNoSeriousA11yViolations(page)
})

test("내가 관리자가 아니면 관리자 행은 모두 꺼져 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/users")
  const drawer = await openUser(page, "admin")
  for (const name of ["이름 바꾸기", "잠그기", "비밀번호 초기화", "삭제"])
    await expect(drawer.getByRole("button", { name })).toBeDisabled()
})

test("비밀번호 없는 계정은 초기화가 꺼져 있다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  const drawer = await openUser(page, "kim")
  await expect(drawer.getByRole("button", { name: "비밀번호 초기화" })).toBeDisabled()
})

test("만들기도 드로어에서 하고 임시 비밀번호를 드로어 안에 한 번 보인다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await page.getByRole("button", { name: "사용자 만들기" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("아이디").fill("new")
  await dialog.getByLabel("이름").fill("새 사용자")
  await dialog.getByRole("button", { name: "저장" }).click()
  // moa: 만들기 드로어 안에 임시 비밀번호를 한 번 보인다 (specs/002-admin-users FR-007)
  await expect(dialog.getByLabel("임시 비밀번호")).toHaveValue("aB3$kP9mQ2xZ")
  await expectNoSeriousA11yViolations(page)
  await dialog.getByRole("button", { name: "확인" }).click()
  await expect(dialog).toBeHidden()
})
