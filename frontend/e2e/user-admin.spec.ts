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

const actions = (page: Page, loginId: string) => page.getByRole("button", { name: `${loginId} 관리` })

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

test("내 행은 역할·잠그기·삭제가 꺼져 있다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await actions(page, "e2e").click()
  await expect(page.getByRole("menuitem", { name: "잠그기" })).toHaveAttribute("aria-disabled", "true")
  await expect(page.getByRole("menuitem", { name: "삭제" })).toHaveAttribute("aria-disabled", "true")
  await expect(page.getByRole("menuitem", { name: "이름 바꾸기" })).not.toHaveAttribute("aria-disabled", "true")
})

test("내가 관리자가 아니면 관리자 행은 모두 꺼져 있다", async ({ page }) => {
  await mock(page, "USER")
  await page.goto("/admin/users")
  await actions(page, "admin").click()
  for (const name of ["이름 바꾸기", "잠그기", "비밀번호 초기화", "삭제"])
    await expect(page.getByRole("menuitem", { name })).toHaveAttribute("aria-disabled", "true")
})

test("비밀번호 없는 계정은 초기화가 꺼져 있다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await actions(page, "kim").click()
  await expect(page.getByRole("menuitem", { name: "비밀번호 초기화" })).toHaveAttribute("aria-disabled", "true")
})

test("만들면 임시 비밀번호를 한 번 보인다", async ({ page }) => {
  await mock(page, "ADMIN")
  await page.goto("/admin/users")
  await page.getByRole("button", { name: "사용자 만들기" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("아이디").fill("new")
  await dialog.getByLabel("이름").fill("새 사용자")
  await dialog.getByRole("button", { name: "저장" }).click()
  const shown = page.getByRole("dialog", { name: "임시 비밀번호" })
  await expect(shown.getByLabel("임시 비밀번호")).toHaveValue("aB3$kP9mQ2xZ")
  await expectNoSeriousA11yViolations(page)
  await shown.getByRole("button", { name: "확인" }).click()
  await expect(shown).toBeHidden()
})
