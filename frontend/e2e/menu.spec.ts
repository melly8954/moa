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
  audience: "ADMIN",
  children: [] as unknown[],
  ...extra,
})
const ADMIN_MENUS = [
  menu(2, "메뉴 관리", "/admin/menus", "menu", 10, { permission: "MENU_MANAGE" }),
  menu(3, "코드 관리", "/admin/codes", "list", 20, { permission: "CODE_MANAGE", enabled: false }),
  menu(7, "업무", null, "folder", 50, {
    children: [menu(8, "주문", "/orders", "cart", 10, { parentId: 7 })],
  }),
  // moa: 사용자 메뉴 (대상 USER, specs/001-admin-menus FR-007)
  menu(101, "홈", "/", "home", 10, { audience: "USER" }),
  menu(102, "공지", "/notices", "megaphone", 20, { audience: "USER" }),
]

async function mock(page: Page, onOrder?: (body: unknown) => void, onCreate?: (body: unknown) => void) {
  await signedIn(page, { permissions: ["MENU_MANAGE"] })
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: MY_MENUS }))
  await page.route("**/api/v1/admin/menus", (route) => {
    if (route.request().method() === "POST") {
      onCreate?.(route.request().postDataJSON())
      return route.fulfill({ status: 201, json: menu(200, "새 메뉴", "/new", "list", 30) })
    }
    return route.fulfill({ json: ADMIN_MENUS })
  })
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

// 관리 셸·사용자 셸 (specs/001-admin-menus FR-001·FR-005·FR-006). 백엔드 대신 API를 가로챈다
const SIX_ADMIN_MENUS = [
  ["사용자 관리", "/admin/users", "users"],
  ["메뉴 관리", "/admin/menus", "menu"],
  ["코드 관리", "/admin/codes", "list"],
  ["접속 이력", "/admin/login-histories", "history"],
  ["역할 관리", "/admin/roles", "shield"],
  ["공지 관리", "/admin/notices", "megaphone"],
].map(([name, path, icon], i) => ({ id: i + 1, name, path, icon, children: [] }))

async function signIn(page: Page, menus: unknown[]) {
  await signedIn(page, { menus })
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  // 첫 화면의 목록 API는 빈 결과로 둔다
  await page.route("**/api/v1/admin/**", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }),
  )
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.goto("/sign-in")
  await page.getByLabel("아이디").fill("e2e")
  await page.getByLabel("비밀번호").fill("e2e-pass")
  await page.getByRole("button", { name: "로그인" }).click()
}

const menuLinks = (page: Page) => page.getByRole("navigation", { name: "메뉴" }).getByRole("link")

test("US1-AC1 관리자는 로그인하면 첫 관리 메뉴 화면이 열리고 사이드바에 관리 메뉴 6개만 있다 @wide", async ({
  page,
}) => {
  await signIn(page, SIX_ADMIN_MENUS)
  await expect(page).toHaveURL(/\/admin\/users$/)
  await expect(menuLinks(page)).toHaveText(SIX_ADMIN_MENUS.map((m) => m.name))
  await expect(page.getByRole("link", { name: "홈", exact: true })).toHaveCount(0)
  await expect(page.getByRole("link", { name: /^공지(, 새 공지)?$/ })).toHaveCount(0)
})

test("US1-AC3 넓은 폭에는 머리 줄이 없고, 로고 줄 단추로 사이드바를 접고 로고 자리 단추로 다시 편다 @wide", async ({
  page,
}) => {
  await signedIn(page, { menus: MY_MENUS })
  await page.route("**/api/v1/admin/**", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }),
  )
  await page.goto("/admin/users")
  await expect(menuLinks(page)).toHaveText(MY_MENUS.map((m) => m.name))
  // 머리 줄(서랍 단추가 있는 줄)은 좁은 폭에만 있다 (FR-005). 사이드바 가장자리(Rail)도 이름이 같아 서랍 단추를 data-sidebar로 찾는다
  await expect(page.locator('[data-sidebar="trigger"]')).toBeHidden()

  await page.getByRole("button", { name: "사이드바 접기" }).click()
  const expand = page.getByRole("button", { name: "사이드바 펼치기" })
  await expect(expand).toBeVisible()
  await expect(page.getByRole("button", { name: "사이드바 접기" })).toHaveCount(0)
  await expectNoSeriousA11yViolations(page)

  await expand.click()
  await expect(page.getByRole("button", { name: "사이드바 접기" })).toBeVisible()
  await expect(menuLinks(page).first()).toHaveText("사용자 관리")
})

test("US2-AC1 일반 회원 사이드바에는 관리 메뉴가 없다 @wide", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.goto("/")
  await expect(menuLinks(page)).toHaveText(["홈", "공지"])
})

test("US2-AC4 일반 회원은 로그인하면 사용자 셸 첫 화면이 열리고 사이드바에 홈, 공지가 있다 @wide", async ({ page }) => {
  await signIn(page, [])
  await expect(page).toHaveURL(/\/$/)
  await expect(menuLinks(page)).toHaveText(["홈", "공지"])
})

test("관리자의 계정 화면(설정)은 관리 셸 안에 열린다 @wide", async ({ page }) => {
  await signedIn(page, { menus: SIX_ADMIN_MENUS })
  await page.goto("/settings")
  await expect(page.getByRole("heading", { name: "설정" })).toBeVisible()
  await expect(menuLinks(page)).toHaveText(SIX_ADMIN_MENUS.map((m) => m.name))
})

test("메뉴 관리는 대상 탭으로 나뉘고, 사용자 메뉴 탭에서 만들면 대상 USER로 보낸다 (FR-007) @wide", async ({
  page,
}) => {
  let created: unknown
  await mock(page, undefined, (body) => (created = body))
  await page.goto("/admin/menus")
  // 관리자 메뉴 탭: 관리 메뉴만
  await expect(page.getByRole("cell", { name: "메뉴 관리", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "홈", exact: true })).toHaveCount(0)

  await page.getByRole("tab", { name: "사용자 메뉴" }).click()
  await expect(page.getByRole("cell", { name: "홈", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "공지", exact: true })).toBeVisible()
  await expect(page.getByRole("cell", { name: "메뉴 관리", exact: true })).toHaveCount(0)

  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByText("대상: 사용자 메뉴", { exact: false })).toBeVisible()
  await dialog.getByLabel("이름").fill("이벤트")
  await dialog.getByLabel("경로").fill("/events")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => created).toMatchObject({ name: "이벤트", path: "/events", audience: "USER" })
})

test("사용자 셸은 서버의 사용자 메뉴를 그린다 (FR-007) @wide", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/menus/user", (route) =>
    route.fulfill({
      json: [
        { id: 101, name: "홈", path: "/", icon: "home", children: [] },
        { id: 103, name: "이벤트", path: "/events", icon: "bell", children: [] },
      ],
    }),
  )
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: false } }))
  await page.goto("/")
  await expect(menuLinks(page)).toHaveText(["홈", "이벤트"])
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
