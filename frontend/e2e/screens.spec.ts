import { expect, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 화면 공통 검사. 화면을 만들면 경로를 더한다
//   publicRoutes: 로그인 없이 보는 화면 / routes: 로그인 뒤 화면 / wideRoutes: 넓은 폭만 지원하는 화면
const publicRoutes = ["/sign-in"]
const routes = [
  "/",
  "/account/password",
  // @helpers:routes
]
const wideRoutes: string[] = []

for (const route of publicRoutes) {
  test(`공개 화면 ${route}: 가로 넘침·접근성`, async ({ page }) => {
    await page.goto(route)
    await expectNoHorizontalOverflow(page)
    await expectNoSeriousA11yViolations(page)
  })
}

for (const route of routes) {
  test(`화면 ${route}: 가로 넘침·접근성`, async ({ page }) => {
    await signedIn(page)
    await page.goto(route)
    await expect(page.getByRole("main")).toBeVisible()
    await expectNoHorizontalOverflow(page)
    await expectNoSeriousA11yViolations(page)
  })
}

for (const route of wideRoutes) {
  test(`화면 @wide ${route}: 가로 넘침·접근성`, async ({ page }) => {
    await signedIn(page)
    await page.goto(route)
    await expectNoHorizontalOverflow(page)
    await expectNoSeriousA11yViolations(page)
  })
}

test("화면 모드를 어둡게 고르면 어두운 화면으로 바뀌고, 다시 열어도 유지된다", async ({ page }) => {
  await signedIn(page)
  await page.goto("/")
  await expect(page.getByRole("main")).toBeVisible()
  // 폭이 좁으면 화면 모드 버튼이 서랍 안에 있어 서랍부터 연다 (상단 메뉴형은 늘 헤더에 있다)
  //   사이드바형: 헤더의 사이드바 버튼(Toggle Sidebar) / 두 겹 사이드바형: 헤더의 메뉴 열기
  const trigger = page.getByRole("button", { name: "화면 모드" })
  if (!(await trigger.isVisible())) {
    await page
      .getByRole("button", { name: /^(Toggle Sidebar|메뉴 열기)$/ })
      .filter({ visible: true })
      .first()
      .click()
  }
  await trigger.click()
  await page.getByRole("menuitemradio", { name: "어둡게" }).click()
  await expect(page.locator("html")).toHaveClass(/\bdark\b/)
  await page.keyboard.press("Escape")
  await expectNoSeriousA11yViolations(page)

  await page.reload()
  await expect(page.locator("html")).toHaveClass(/\bdark\b/)
})

test("로그인하지 않았으면 로그인 화면으로 보낸다", async ({ page }) => {
  await signedOut(page)
  await page.goto("/")
  await expect(page).toHaveURL(/\/sign-in$/)
})
