import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 메뉴 (menu 헬퍼): 셸이 서버 메뉴를 그리는지, 메뉴 관리 화면. 백엔드 대신 API를 가로챈다

// 키트 시드처럼 관리 메뉴는 묶음(그룹) 없이 최상위에 둔다. "업무"는 하위 메뉴가 있는 묶음 예시다
const MY_MENUS = [
  { id: 6, name: "사용자 관리", path: "/admin/users", icon: "users", children: [] },
  { id: 2, name: "메뉴 관리", path: "/admin/menus", icon: "menu", children: [] },
]
const menu = (id: number, name: string, path: string | null, icon: string, sortOrder: number, extra = {}) => ({
  id,
  parentId: null as number | null,
  name,
  path,
  icon,
  sortOrder,
  enabled: true,
  permission: null as string | null,
  children: [] as unknown[],
  ...extra,
})
const ADMIN_MENUS = [
  menu(2, "메뉴 관리", "/admin/menus", "menu", 10, { permission: "MENU_MANAGE" }),
  menu(3, "코드 관리", "/admin/codes", "list", 20, { permission: "CODE_MANAGE", enabled: false }),
  menu(7, "업무", null, "folder", 50, {
    children: [menu(8, "주문", "/orders", "cart", 10, { parentId: 7 })],
  }),
]

async function mock(page: Page, onOrder?: (body: unknown) => void) {
  await signedIn(page, { permissions: ["MENU_MANAGE"] })
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: MY_MENUS }))
  await page.route("**/api/v1/admin/menus", (route) => route.fulfill({ json: ADMIN_MENUS }))
  await page.route("**/api/v1/admin/menus/order", (route) => {
    onOrder?.(route.request().postDataJSON())
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/menus/7", (route) =>
    route.fulfill({ status: 409, json: { code: "B001", message: "", timestamp: "", path: "" } }),
  )
}

test("메뉴 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await expect(page.getByRole("cell", { name: "코드 관리", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("셸은 코드 메뉴(홈) 다음에 서버 메뉴를 그린다 @wide", async ({ page }) => {
  await mock(page)
  await page.goto("/")
  await expect(page.getByRole("link", { name: "홈", exact: true }).first()).toBeVisible()
  // 서버 메뉴는 코드 메뉴 다음에 최상위 링크로 보인다 (두 겹 사이드바형은 띠의 아이콘 링크, 이름은 aria-label)
  const nav = page.getByRole("navigation")
  await expect(nav.getByRole("link", { name: "사용자 관리", exact: true }).first()).toBeAttached()
  await expect(nav.getByRole("link", { name: "메뉴 관리", exact: true }).first()).toBeAttached()
})

test("아래로 옮기면 그 단계 순서를 한 번에 보낸다", async ({ page }) => {
  let sent: unknown
  await mock(page, (body) => (sent = body))
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "메뉴 관리 아래로" }).click()
  await expect
    .poll(() => sent)
    .toEqual([
      { id: 3, parentId: null, sortOrder: 10 },
      { id: 2, parentId: null, sortOrder: 20 },
      { id: 7, parentId: null, sortOrder: 30 },
    ])
})

test("경로는 /로 시작해야 한다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("이름").fill("외부")
  await dialog.getByLabel("경로").fill("https://example.com")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog.getByText("/로 시작하는 화면 주소를 적거나 비워 주세요.", { exact: false })).toBeVisible()
})

test("하위 메뉴가 있는 메뉴를 지우면(B001) 그렇게 알린다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "업무 삭제", exact: true }).click()
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제" }).click()
  await expect(page.getByText("하위 메뉴가 있는 메뉴는 지울 수 없어요.")).toBeVisible()
})
