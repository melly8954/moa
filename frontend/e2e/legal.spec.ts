import { expect, type Page, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"
import { mockLegal } from "./support/legal"

// 이용약관·개인정보처리방침 (specs/012-terms-privacy). 백엔드 대신 공개 문서 API를 가로챈다
// 문서 고르기·상태·404 규칙은 백엔드 LegalAcceptanceTest·LegalServiceTest가 본다
//   여기서는 화면에서만 보이는 것을 본다 (research R8, contracts/api.md "AC와 확인 방법")

const footer = (page: Page) => page.getByRole("navigation", { name: "약관과 방침" }).last()

async function adminAccount(page: Page) {
  await signedIn(page, { me: { role: "ADMIN" } })
}

test("US1-AC3 관리자 계정도 개인정보처리방침을 연다 (다른 곳으로 보내지 않는다)", async ({ page }) => {
  await adminAccount(page)
  await mockLegal(page)
  await page.goto("/privacy")
  await expect(page.getByRole("heading", { level: 1, name: "moa 개인정보처리방침" })).toBeVisible()
  await expect(page).toHaveURL(/\/privacy$/)
})

test("US1-AC4 처리방침 전문이 보이고 표만 가로로 넘기며, 화면 폭을 넘지 않고 접근성 위반이 없다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.goto("/privacy")
  await expect(page.getByText("시행일 2026-11-01 · 버전 draft")).toBeVisible()
  const table = page.locator("table")
  await expect(table).toBeVisible()
  await expect(table).toHaveCSS("overflow-x", "auto")
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US1-AC5 목차의 장 제목을 누르면 그 장으로 간다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.goto("/privacy")
  await page.getByRole("navigation", { name: "목차" }).getByRole("link", { name: "보유 기간" }).click()
  await expect(page).toHaveURL(/\/privacy#retention$/)
  await expect(page.getByRole("heading", { level: 2, name: "보유 기간" })).toBeInViewport()
})

test("US1-AC6 머리 줄에서 다른 문서로 가고, 관리자 계정이 moa를 누르면 관리 화면으로 간다", async ({ page }) => {
  await adminAccount(page)
  await mockLegal(page)
  await page.goto("/privacy")
  const header = page.getByRole("navigation", { name: "공개 문서" })
  await expect(header.getByRole("link", { name: "개인정보처리방침" })).toHaveAttribute("aria-current", "page")
  await header.getByRole("link", { name: "이용약관" }).click()
  await expect(page).toHaveURL(/\/terms$/)
  await expect(page.getByRole("heading", { level: 1, name: "moa 이용약관" })).toBeVisible()

  await page.getByRole("link", { name: "moa", exact: true }).click()
  await expect(page).toHaveURL(/\/admin$/)
})

test("US2-AC1 로그인 화면 맨 아래 푸터 줄로 문서를 연다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.route("**/api/v1/auth/oauth/providers", (route) => route.fulfill({ json: { providers: [] } }))
  await page.goto("/sign-in")
  await footer(page).getByRole("link", { name: "개인정보처리방침" }).click()
  await expect(page).toHaveURL(/\/privacy$/)
  await expect(page.getByRole("heading", { level: 1, name: "moa 개인정보처리방침" })).toBeVisible()
})

test("US2-AC2·AC3 사용자 셸 본문 맨 아래에 푸터 줄이 있고 처리방침이 굵다", async ({ page }) => {
  await signedIn(page)
  await mockLegal(page)
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.goto("/")
  const privacy = footer(page).getByRole("link", { name: "개인정보처리방침" })
  await expect(privacy).toBeVisible()
  const weight = await privacy.evaluate((el) => Number(getComputedStyle(el).fontWeight))
  expect(weight).toBeGreaterThanOrEqual(600)
  // 본문이 짧아도 화면 맨 아래에 붙는다
  const box = await footer(page).boundingBox()
  const viewport = page.viewportSize()
  expect(box && viewport && box.y + box.height).toBeGreaterThan((viewport?.height ?? 0) - 80)
  await privacy.click()
  await expect(page).toHaveURL(/\/privacy$/)
})

test("US2-AC2 사이드바를 접어도 사용자 셸 푸터 줄은 본문 맨 아래에 있다 @wide", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.goto("/")
  await page.getByRole("button", { name: "사이드바 접기" }).click()
  await expect(footer(page).getByRole("link", { name: "이용약관" })).toBeVisible()
})

test("US2-AC5 관리 셸에는 푸터 줄이 없다 @wide", async ({ page }) => {
  await adminAccount(page)
  await page.goto("/admin")
  await expect(page.getByRole("heading", { name: "받은 권한이 없어요" })).toBeVisible()
  await expect(page.getByRole("link", { name: "개인정보처리방침" })).toHaveCount(0)
})

test("US3-AC1·AC3 시행 예정 안내 띠와 버전 목록이 보이고, 예정 버전을 미리 본다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.goto("/privacy")
  await expect(page.getByText("2099년 1월 1일부터 바뀝니다.")).toBeVisible()
  await page.getByRole("button", { name: "다른 버전 보기" }).click()
  const list = page.getByRole("list").filter({ hasText: "지금 시행 중" })
  await expect(list.getByRole("link")).toHaveText([/2099-01-01.*예정/, /2026-11-01.*지금 시행 중/, /2025-01-01/])

  await page.getByRole("link", { name: "바뀌는 내용 미리 보기" }).click()
  await expect(page).toHaveURL(/\/privacy\/versions\/next$/)
  await expect(page.getByText("2099년 1월 1일부터 시행돼요. 아직 시행 전이에요.")).toBeVisible()
})

test("US3-AC2 예전 버전을 열면 지금 버전이 아니라는 안내와 지금 버전 링크가 있다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.goto("/terms/versions/old")
  await expect(page.getByText("terms old 본문입니다.")).toBeVisible()
  await expect(page.getByText("지금 시행 중인 버전이 아니에요.")).toBeVisible()
  await page.getByRole("link", { name: "지금 버전 보기" }).click()
  await expect(page).toHaveURL(/\/terms$/)
})

test("없는 버전을 열면 없는 버전 안내와 지금 버전 링크가 있다", async ({ page }) => {
  await signedOut(page)
  await mockLegal(page)
  await page.goto("/privacy/versions/none")
  await expect(page.getByRole("heading", { name: "없는 버전이에요" })).toBeVisible()
  await expect(page.getByRole("link", { name: "지금 버전 보기" })).toHaveAttribute("href", "/privacy")
})
