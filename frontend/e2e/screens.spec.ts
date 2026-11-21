import { expect, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 화면 공통 검사. 화면을 만들면 경로를 더한다
//   publicRoutes: 로그인 없이 보는 화면 / routes: 로그인 뒤 화면 / wideRoutes: 넓은 폭만 지원하는 화면
const publicRoutes = ["/sign-in"]
const routes = [
  "/",
  "/account/password",
  "/settings",
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
  // 로그인했으면 화면 모드는 계정 줄 톱니("설정 메뉴") 안에 있다 (moa 001 FR-005, 2026-11-21)
  // 폭이 좁으면 계정 줄이 서랍 안에 있어 서랍부터 연다 (상단 메뉴형은 늘 헤더에 있다)
  //   사이드바형: 헤더의 사이드바 버튼(Toggle Sidebar) / 두 겹 사이드바형: 헤더의 메뉴 열기
  const trigger = page.getByRole("button", { name: "설정 메뉴" })
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

test("계정 줄은 한 줄에 이름·아이디와 알림·설정 메뉴·로그아웃 단추를 두고, 설정 메뉴는 화면 모드·설정·비밀번호 변경이다 (moa 001 FR-005) @wide", async ({
  page,
}) => {
  await signedIn(page)
  await page.goto("/")
  const footer = page.locator('[data-slot="sidebar-footer"]')
  await expect(footer.getByRole("button", { name: /^알림/ })).toBeVisible()
  await expect(footer.getByRole("button", { name: "로그아웃" })).toBeVisible()
  // 화면 모드와 계정 메뉴는 따로 줄을 두지 않는다
  await expect(footer.getByRole("button", { name: "화면 모드" })).toHaveCount(0)
  await footer.getByRole("button", { name: "설정 메뉴" }).click()
  const menu = page.getByRole("menu")
  await expect(menu.getByRole("menuitemradio")).toHaveCount(3)
  await expect(menu.getByRole("menuitem", { name: "설정" })).toHaveAttribute("href", "/settings")
  await expect(menu.getByRole("menuitem", { name: "비밀번호 변경" })).toHaveAttribute("href", "/account/password")
  await expect(menu.getByRole("menuitem", { name: "로그아웃" })).toHaveCount(0)
  await page.keyboard.press("Escape")
  await expectNoSeriousA11yViolations(page)
})

// moa (001 FR-005, 2026-11-22): 이름 칸은 알림 벨 왼쪽까지 남는 칸을 모두 쓰고, 넘치면 말줄임하며 툴팁으로 전체를 보인다
test("계정 줄 이름이 길면 말줄임하고, 마우스를 올리면 툴팁으로 전체 이름과 아이디를 보인다 (moa 001 FR-005) @wide", async ({
  page,
}) => {
  const name = "운영자 (접속 이력만) 아주 긴 이름"
  const loginId = "operator-with-a-long-login-id"
  await signedIn(page, { me: { name, loginId } })
  await page.goto("/")
  const footer = page.locator('[data-slot="sidebar-footer"]')
  const shown = footer.getByText(name, { exact: true })
  await expect(shown).toBeVisible()
  expect(await shown.evaluate((el) => el.scrollWidth > el.clientWidth)).toBe(true)
  // 이름 칸 오른쪽 끝은 알림 벨 바로 왼쪽이다
  const nameRight = await shown.evaluate((el) => el.getBoundingClientRect().right)
  const bellLeft = await footer.getByRole("button", { name: /^알림/ }).evaluate((el) => el.getBoundingClientRect().left)
  expect(bellLeft - nameRight).toBeLessThanOrEqual(8)

  await shown.hover()
  // base-ui 툴팁 내용에는 tooltip 역할이 없어 data-slot으로 찾는다
  const tooltip = page.locator('[data-slot="tooltip-content"]')
  await expect(tooltip).toContainText(name)
  await expect(tooltip).toContainText(loginId)
})

test("관리자 계정이 사용자 화면(공지)을 열면 관리 셸 첫 화면으로 보낸다 (moa 001 FR-005)", async ({ page }) => {
  await signedIn(page, {
    me: { role: "ADMIN" },
    menus: [{ id: 1, name: "사용자 관리", path: "/admin/users", icon: "users", children: [] }],
  })
  await page.goto("/notices")
  await expect(page).toHaveURL(/\/admin\/users$/)
})

test("로그인하지 않았으면 로그인 화면으로 보낸다", async ({ page }) => {
  await signedOut(page)
  await page.goto("/")
  await expect(page).toHaveURL(/\/sign-in$/)
})
