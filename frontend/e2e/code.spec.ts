import { existsSync } from "node:fs"
import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 코드 관리 (code 헬퍼). 백엔드 대신 API를 가로챈다

const GROUPS = [
  { groupCode: "ORDER_STATUS", name: "주문 상태", description: null },
  { groupCode: "BANK", name: "은행", description: "환불 계좌" },
]
const CODES = [
  { code: "PAID", name: "결제 완료", sortOrder: 10, enabled: true },
  { code: "CANCELED", name: "취소", sortOrder: 20, enabled: false },
]

async function mockCodes(page: Page) {
  await signedIn(page, { permissions: ["CODE_MANAGE"] })
  await page.route("**/api/v1/admin/code-groups", (route) => route.fulfill({ json: GROUPS }))
  await page.route("**/api/v1/admin/code-groups/*/codes", (route) =>
    route.request().method() === "POST"
      ? route.fulfill({
          status: 409,
          json: { code: "R002", message: "", timestamp: "", path: "" },
        })
      : route.fulfill({ json: CODES }),
  )
}

test("코드 관리: 가로 넘침·접근성", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes?group=ORDER_STATUS")
  await expect(page.getByRole("cell", { name: "결제 완료", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

// menu 헬퍼가 있으면 메뉴는 서버가 준다 (menu.ts에 코드 관리를 더하지 않는다). 그때는 메뉴 검사를 건너뛴다
const SERVER_MENU = existsSync("src/types/menu.ts")

test("권한이 있으면 메뉴에 코드 관리가 보인다 @wide", async ({ page }) => {
  test.skip(SERVER_MENU, "메뉴는 서버가 준다 (menu 헬퍼)")
  await mockCodes(page)
  await page.goto("/")
  await expect(page.getByRole("link", { name: "코드 관리" }).first()).toBeVisible()
})

test("권한이 없으면 메뉴에 코드 관리가 없다 @wide", async ({ page }) => {
  test.skip(SERVER_MENU, "메뉴는 서버가 준다 (menu 헬퍼)")
  await signedIn(page, { permissions: [] })
  await page.goto("/")
  await expect(page.getByRole("link", { name: "홈", exact: true }).first()).toBeVisible()
  await expect(page.getByRole("link", { name: "코드 관리" })).toHaveCount(0)
})

test("그룹을 고르면 그 그룹의 코드가 보이고, 사용 안 함도 표시한다", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: /주문 상태/ }).click()
  await expect(page).toHaveURL(/group=ORDER_STATUS/)
  await expect(page.getByRole("row", { name: /취소/ })).toContainText("사용 안 함")
})

test("코드 추가: 형식이 틀리면 칸에, 이미 있으면(R002) 코드 칸에 알린다", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes?group=ORDER_STATUS")
  await page.getByRole("button", { name: "코드 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("코드").fill("paid")
  await dialog.getByLabel("이름").fill("결제 완료")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog.getByText("영문 대문자·숫자·밑줄(_)로 적어 주세요.")).toBeVisible()

  await dialog.getByLabel("코드").fill("PAID")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog.getByText("이미 있는 코드예요.")).toBeVisible()
})

test("삭제는 되묻는다", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes?group=ORDER_STATUS")
  await page.getByRole("button", { name: "결제 완료 삭제" }).click()
  await expect(page.getByRole("alertdialog", { name: "결제 완료 코드를 지울까요?" })).toBeVisible()
  await page.getByRole("button", { name: "취소" }).click()
})
