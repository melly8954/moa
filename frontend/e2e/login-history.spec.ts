import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 접속 이력 (login-history 헬퍼). 백엔드 대신 API를 가로챈다

const ROWS = [
  {
    id: 2,
    userId: null,
    loginId: "kim",
    success: false,
    failureCode: "A006",
    ip: "10.0.0.2",
    deviceType: "MOBILE",
    os: "Android",
    browser: "Chrome",
    createdAt: "2026-11-07T09:10:05+09:00",
  },
  {
    id: 1,
    userId: 3,
    loginId: "kim",
    success: true,
    failureCode: null,
    ip: "10.0.0.1",
    deviceType: "PC",
    os: "Windows",
    browser: "Edge",
    createdAt: "2026-11-07T09:00:00+09:00",
  },
]

async function mock(page: Page, requests: URL[] = []) {
  await signedIn(page, { permissions: ["LOGIN_HISTORY_READ"] })
  await page.route("**/api/v1/admin/login-histories*", (route) => {
    requests.push(new URL(route.request().url()))
    return route.fulfill({ json: { content: ROWS, page: 0, size: 20, totalElements: 2, totalPages: 1 } })
  })
}

test("접속 이력: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/login-histories")
  await expect(page.getByText("총 2건")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("실패는 짧은 사유로, 시각은 초까지 보인다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/login-histories")
  await expect(page.getByRole("row", { name: /10\.0\.0\.2/ })).toContainText("비밀번호 불일치")
  await expect(page.getByRole("row", { name: /10\.0\.0\.2/ })).toContainText("2026-11-07 09:10:05")
  await expect(page.getByRole("row", { name: /10\.0\.0\.1/ })).toContainText("성공")
})

test("조건(결과·아이디)을 API에 넘긴다", async ({ page }) => {
  const requests: URL[] = []
  await mock(page, requests)
  await page.goto("/admin/login-histories")
  await page.getByRole("button", { name: "실패", exact: true }).click()
  await page.getByLabel("아이디").fill("kim")
  await page.getByRole("button", { name: "조회" }).click()
  await expect(page).toHaveURL(/success=false/)
  await expect
    .poll(() => requests.at(-1)?.searchParams.toString())
    .toMatch(/success=false.*loginId=kim|loginId=kim.*success=false/)
})
