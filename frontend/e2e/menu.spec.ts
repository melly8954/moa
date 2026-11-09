import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 메뉴 (menu 헬퍼): 셸이 서버 메뉴를 그리는지, 메뉴 관리 화면. 백엔드 대신 API를 가로챈다

const MY_MENUS = [
  {
    id: 1,
    name: "관리",
    path: null,
    icon: "settings",
    children: [{ id: 2, name: "메뉴 관리", path: "/admin/menus", icon: "menu", children: [] }],
  },
]
const ADMIN_MENUS = [
  {
    id: 1,
    parentId: null,
    name: "관리",
    path: null,
    icon: "settings",
    sortOrder: 10,
    enabled: true,
    permission: null,
    children: [
      {
        id: 2,
        parentId: 1,
        name: "메뉴 관리",
        path: "/admin/menus",
        icon: "menu",
        sortOrder: 10,
        enabled: true,
        permission: "MENU_MANAGE",
        children: [],
      },
      {
        id: 3,
        parentId: 1,
        name: "코드 관리",
        path: "/admin/codes",
        icon: "list",
        sortOrder: 20,
        enabled: false,
        permission: "CODE_MANAGE",
        children: [],
      },
    ],
  },
]

async function mock(page: Page, onOrder?: (body: unknown) => void) {
  await signedIn(page, { permissions: ["MENU_MANAGE"] })
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: MY_MENUS }))
  await page.route("**/api/v1/admin/menus", (route) => route.fulfill({ json: ADMIN_MENUS }))
  await page.route("**/api/v1/admin/menus/order", (route) => {
    onOrder?.(route.request().postDataJSON())
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/menus/1", (route) =>
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
  // 셸마다 그룹을 보이는 모양이 달라(두 겹 사이드바형은 띠의 버튼) 서버가 준 그룹 이름으로 확인한다
  //   사이드바형·두 겹 사이드바형은 버튼, 상단 메뉴형은 첫 하위 메뉴로 가는 링크
  const nav = page.getByRole("navigation")
  const group = nav
    .getByRole("button", { name: "관리", exact: true })
    .or(nav.getByRole("link", { name: "관리", exact: true }))
  await expect(group.first()).toBeAttached()
})

test("아래로 옮기면 그 단계 순서를 한 번에 보낸다", async ({ page }) => {
  let sent: unknown
  await mock(page, (body) => (sent = body))
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "메뉴 관리 아래로" }).click()
  await expect
    .poll(() => sent)
    .toEqual([
      { id: 3, parentId: 1, sortOrder: 10 },
      { id: 2, parentId: 1, sortOrder: 20 },
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
  await page.getByRole("button", { name: "관리 삭제", exact: true }).click()
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제" }).click()
  await expect(page.getByText("하위 메뉴가 있는 메뉴는 지울 수 없어요.")).toBeVisible()
})
