import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 역할 관리 (rbac 헬퍼). 백엔드 대신 API를 가로챈다

const PERMISSIONS = [
  { key: "CODE_MANAGE", name: "공통코드 관리", group: "공통코드" },
  { key: "ROLE_MANAGE", name: "역할 관리", group: "권한" },
]
const ROLES = [
  {
    id: 1,
    code: "MANAGER",
    name: "운영 매니저",
    description: "코드를 관리한다",
    permissions: ["CODE_MANAGE"],
    userCount: 3,
  },
]

async function mock(page: Page, onSave?: (body: unknown) => { status: number; json: unknown }) {
  await signedIn(page, { permissions: ["ROLE_MANAGE"] })
  await page.route("**/api/v1/admin/permissions", (route) => route.fulfill({ json: PERMISSIONS }))
  await page.route("**/api/v1/admin/roles", (route) => {
    if (route.request().method() === "POST") {
      const reply = onSave?.(route.request().postDataJSON()) ?? { status: 201, json: ROLES[0] }
      return route.fulfill(reply)
    }
    return route.fulfill({ json: ROLES })
  })
}

test("역할 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("cell", { name: "운영 매니저", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("역할 추가: 권한을 묶음별로 골라 보낸다", async ({ page }) => {
  let sent: unknown
  await mock(page, (body) => {
    sent = body
    return { status: 201, json: ROLES[0] }
  })
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("역할 코드").fill("VIEWER")
  await dialog.getByLabel("이름").fill("조회 담당")
  await dialog.getByRole("checkbox", { name: "공통코드 관리" }).click()
  await expectNoSeriousA11yViolations(page)
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog).toBeHidden()
  expect(sent).toEqual({ code: "VIEWER", name: "조회 담당", permissions: ["CODE_MANAGE"] })
})

test("내 권한 밖이면(A005) 그렇게 알린다", async ({ page }) => {
  await mock(page, () => ({ status: 403, json: { code: "A005", message: "", timestamp: "", path: "" } }))
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("역할 코드").fill("SUPER")
  await dialog.getByLabel("이름").fill("최고")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog.getByText("내가 가진 권한 밖의 권한은 다룰 수 없어요.")).toBeVisible()
})

test("삭제는 사용자 수를 알리고 되묻는다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "운영 매니저 삭제" }).click()
  await expect(page.getByRole("alertdialog")).toContainText("3명")
  await page.getByRole("button", { name: "취소" }).click()
})
