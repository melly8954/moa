import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 메뉴 (menu 헬퍼): 셸이 서버 메뉴를 그리는지, 메뉴 관리 화면. 백엔드 대신 API를 가로챈다

// 키트 시드처럼 관리 메뉴는 묶음(그룹) 없이 최상위에 둔다. "업무"는 하위 메뉴가 있는 묶음 예시다
const MY_MENUS = [
  { id: 6, name: "사용자 관리", path: "/admin/users", icon: "users", children: [] },
  { id: 2, name: "메뉴 관리", path: "/admin/menus", icon: "menu", children: [] },
]
// moa: 메뉴는 등록된 프로그램을 가리킨다 (specs/001-admin-menus FR-008·FR-009)
const PROGRAMS = [
  {
    key: "ADMIN_MENUS",
    name: "메뉴 관리",
    path: "/admin/menus",
    audience: "ADMIN",
    permission: { key: "MENU_MANAGE", name: "메뉴 관리" },
    signInRequired: true,
  },
  {
    key: "ADMIN_CODES",
    name: "코드 관리",
    path: "/admin/codes",
    audience: "ADMIN",
    permission: { key: "CODE_MANAGE", name: "공통코드 관리" },
    signInRequired: true,
  },
  { key: "HOME", name: "홈", path: "/", audience: "USER", permission: null, signInRequired: false },
  { key: "NOTICES", name: "공지", path: "/notices", audience: "USER", permission: null, signInRequired: false },
  { key: "MY_PAGE", name: "내 정보", path: "/me", audience: "USER", permission: null, signInRequired: true },
]
const menu = (id: number, name: string, programKey: string | null, icon: string, sortOrder: number, extra = {}) => {
  const program = PROGRAMS.find((p) => p.key === programKey)
  return {
    id,
    parentId: null as number | null,
    name,
    icon,
    sortOrder,
    enabled: true,
    audience: program?.audience ?? "ADMIN",
    programKey,
    program: program
      ? {
          name: program.name,
          path: program.path,
          permission: program.permission?.key ?? null,
          permissionName: program.permission?.name ?? null,
          signInRequired: program.signInRequired,
        }
      : null,
    missingProgram: false,
    visibility: program?.audience === "USER" ? "PUBLIC" : null,
    children: [] as unknown[],
    ...extra,
  }
}
// moa: 관리 메뉴는 묶음 아래에 둔다 (FR-001). "운영"은 하위 메뉴가 있는 묶음, "설정"은 빈 묶음, "코드 관리"는 1단계 화면 메뉴
const ADMIN_MENUS = [
  menu(7, "운영", null, "briefcase", 10, {
    children: [
      menu(8, "코드", "ADMIN_CODES", "list", 10, { parentId: 7 }),
      menu(9, "메뉴", "ADMIN_MENUS", "menu", 20, { parentId: 7 }),
    ],
  }),
  menu(3, "코드 관리", "ADMIN_CODES", "list", 20, { enabled: false }),
  menu(10, "설정", null, "settings", 30),
  // moa: 사용자 메뉴 (대상 USER, specs/001-admin-menus FR-007)
  menu(101, "홈", "HOME", "home", 10),
  menu(102, "공지", "NOTICES", "megaphone", 20),
]

type Sent = { order?: unknown; create?: unknown; modify?: unknown; enabled?: unknown; bulkDelete?: unknown }

async function mock(page: Page, sent: Sent = {}) {
  await signedIn(page, { permissions: ["MENU_MANAGE"] })
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: MY_MENUS }))
  await page.route("**/api/v1/admin/programs", (route) => route.fulfill({ json: PROGRAMS }))
  await page.route("**/api/v1/admin/menus", (route) => {
    if (route.request().method() === "POST") {
      sent.create = route.request().postDataJSON()
      return route.fulfill({ status: 201, json: menu(200, "새 메뉴", "NOTICES", "list", 30) })
    }
    return route.fulfill({ json: ADMIN_MENUS })
  })
  await page.route("**/api/v1/admin/menus/order", (route) => {
    sent.order = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/menus/enabled", (route) => {
    sent.enabled = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  // 하위 메뉴가 있는 묶음(7)이 섞이면 B001
  await page.route("**/api/v1/admin/menus/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    const ids = (sent.bulkDelete as { ids: number[] }).ids
    return ids.includes(7) && !(ids.includes(8) && ids.includes(9))
      ? route.fulfill({ status: 409, json: { code: "B001", message: "", timestamp: "", path: "" } })
      : route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/menus/3", (route) => {
    sent.modify = route.request().postDataJSON()
    return route.fulfill({ json: menu(3, "코드", "ADMIN_CODES", "list", 20) })
  })
}

const nameButton = (page: Page, name: string) => page.getByRole("button", { name, exact: true })

test("메뉴 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await expect(nameButton(page, "코드 관리")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

// 관리 셸·사용자 셸 (specs/001-admin-menus FR-001·FR-005·FR-006). 백엔드 대신 API를 가로챈다
// moa: 처음 구성은 운영·설정 묶음 아래 여섯 메뉴 (FR-001)
const sub = (names: [string, string, string][], offset: number) =>
  names.map(([name, path, icon], i) => ({ id: offset + i, name, path, icon, children: [] }))
const SIX_ADMIN_MENUS = [
  {
    id: 100,
    name: "운영",
    path: null,
    icon: "briefcase",
    children: sub(
      [
        ["사용자 관리", "/admin/users", "users"],
        ["공지 관리", "/admin/notices", "megaphone"],
        ["접속 이력", "/admin/login-histories", "history"],
      ],
      1,
    ),
  },
  {
    id: 200,
    name: "설정",
    path: null,
    icon: "settings",
    children: sub(
      [
        ["역할 관리", "/admin/roles", "shield"],
        ["메뉴 관리", "/admin/menus", "menu"],
        ["공통코드", "/admin/codes", "list"],
      ],
      4,
    ),
  },
]
const SIX_NAMES = SIX_ADMIN_MENUS.flatMap((g) => g.children.map((m) => m.name))

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
// 사이드바 묶음 행 (눌러 펴고 접는다, FR-005)
const menuGroup = (page: Page, name: string) =>
  page.getByRole("navigation", { name: "메뉴" }).getByRole("button", { name, exact: true })
const OPS_NAMES = SIX_ADMIN_MENUS[0].children.map((m) => m.name)

// 관리 화면의 API는 비운다 (사이드바만 본다). 공통코드는 목록이 아니라 그룹 트리(배열)를 받는다
async function adminShell(page: Page) {
  await signedIn(page, { menus: SIX_ADMIN_MENUS })
  await page.route("**/api/v1/admin/**", (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }),
  )
  await page.route("**/api/v1/admin/code-groups/tree", (route) => route.fulfill({ json: [] }))
}

test("US1-AC1 관리자는 로그인하면 첫 관리 메뉴 화면이 열리고 사이드바에 운영·설정 묶음 아래 관리 메뉴 6개만 있다 @wide", async ({
  page,
}) => {
  await signIn(page, SIX_ADMIN_MENUS)
  await expect(page).toHaveURL(/\/admin\/users$/)
  // 지금 화면이 있는 "운영"만 펴져 있고, "설정"은 펴야 보인다 (FR-005)
  await expect(menuGroup(page, "운영")).toHaveAttribute("aria-expanded", "true")
  await expect(menuGroup(page, "설정")).toHaveAttribute("aria-expanded", "false")
  await expect(menuLinks(page)).toHaveText(OPS_NAMES)
  await menuGroup(page, "설정").click()
  await expect(menuLinks(page)).toHaveText(SIX_NAMES)
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

test("US1-AC4 묶음을 펴고 접은 상태는 새로 고쳐도 유지된다 @wide", async ({ page }) => {
  await adminShell(page)
  await page.goto("/admin/users")
  const settings = menuGroup(page, "설정")
  await expect(settings).toHaveAttribute("aria-expanded", "false")
  await expect(page.getByRole("link", { name: "메뉴 관리" })).toHaveCount(0)

  await settings.click()
  await expect(settings).toHaveAttribute("aria-expanded", "true")
  await page.reload()
  await expect(settings).toHaveAttribute("aria-expanded", "true")
  await expect(page.getByRole("link", { name: "메뉴 관리" })).toBeVisible()

  await settings.click()
  await expect(settings).toHaveAttribute("aria-expanded", "false")
  await page.reload()
  await expect(settings).toHaveAttribute("aria-expanded", "false")
  await expect(page.getByRole("link", { name: "메뉴 관리" })).toHaveCount(0)
})

test("US1-AC5 기억이 없으면 묶음 안의 화면 주소로 들어갈 때 그 묶음만 펴진다 @wide", async ({ page }) => {
  await adminShell(page)
  await page.goto("/admin/codes")
  await expect(menuGroup(page, "설정")).toHaveAttribute("aria-expanded", "true")
  await expect(menuGroup(page, "운영")).toHaveAttribute("aria-expanded", "false")
  await expect(page.getByRole("link", { name: "공통코드" })).toHaveAttribute("data-active", /.*/)
  await expectNoSeriousA11yViolations(page)
})

test("US1-AC6 지금 화면의 묶음도 직접 접으면 새로 고쳐도 접힌 채다 @wide", async ({ page }) => {
  await adminShell(page)
  await page.goto("/admin/users")
  const ops = menuGroup(page, "운영")
  await expect(ops).toHaveAttribute("aria-expanded", "true")
  await ops.click()
  await expect(ops).toHaveAttribute("aria-expanded", "false")
  await page.reload()
  await expect(ops).toHaveAttribute("aria-expanded", "false")
  await expect(page.getByRole("link", { name: "사용자 관리" })).toHaveCount(0)
})

test("US1-AC7 아이콘만 남게 접힌 사이드바에서 묶음을 누르면 사이드바가 펼쳐지고 그 묶음이 펴진다 @wide", async ({
  page,
}) => {
  await adminShell(page)
  await page.goto("/admin/users")
  await page.getByRole("button", { name: "사이드바 접기" }).click()
  await expect(page.getByRole("button", { name: "사이드바 펼치기" })).toBeVisible()
  await menuGroup(page, "설정").click()
  await expect(page.getByRole("button", { name: "사이드바 접기" })).toBeVisible()
  await expect(menuGroup(page, "설정")).toHaveAttribute("aria-expanded", "true")
  await expect(page.getByRole("link", { name: "메뉴 관리" })).toBeVisible()
})

test("US1-AC8 다른 묶음의 화면으로 옮겨도 지나온 묶음은 펴진 채다 @wide", async ({ page }) => {
  await adminShell(page)
  await page.goto("/admin/users")
  await menuGroup(page, "설정").click()
  await page.getByRole("link", { name: "공통코드" }).click()
  await expect(page).toHaveURL(/\/admin\/codes$/)
  await expect(menuGroup(page, "운영")).toHaveAttribute("aria-expanded", "true")
  await expect(menuLinks(page)).toHaveText(SIX_NAMES)
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
  // 묶음 안의 화면이 아니라 두 묶음 모두 접혀 있다 (FR-005)
  await expect(menuGroup(page, "운영")).toHaveAttribute("aria-expanded", "false")
  await expect(menuGroup(page, "설정")).toHaveAttribute("aria-expanded", "false")
  await expect(menuLinks(page)).toHaveCount(0)
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

// 메뉴 관리 화면 (moa, specs/001-admin-menus US4·US5, FR-009~FR-012, research R26)
// 넓은 폭(@wide)은 왼쪽 트리 · 오른쪽 상세 칸(region), 좁은 폭은 트리 + 드로어(dialog)다
const detail = (page: Page, name: "메뉴 수정" | "메뉴 추가") => page.getByRole("region", { name })

test("US5-AC1 왼쪽은 상위 메뉴 아래 하위 메뉴를 들여 쓴 트리이고 행 단추·프로그램 탭이 없으며, 오른쪽은 고르라는 안내다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/menus")
  await expect(nameButton(page, "운영")).toBeVisible()
  await expect(nameButton(page, "코드")).toBeVisible()
  await expect(page.getByText("왼쪽에서 메뉴를 고르면 여기서 고쳐요.", { exact: false })).toBeVisible()
  // 일괄 처리 줄은 고르기 전에도 같은 자리에 있다
  const toolbar = page.getByRole("toolbar", { name: "고른 메뉴 처리" })
  await expect(toolbar.getByText("체크해서 한꺼번에 처리해요.")).toBeVisible()
  await expect(toolbar.getByRole("button", { name: "사용 안 함" })).toBeDisabled()
  // 화면에 "묶음"·"프로그램"이라는 말이 없다 (FR-009)
  await expect(page.getByText(/묶음|프로그램/)).toHaveCount(0)
  await expect(page.getByRole("button", { name: /수정$/ })).toHaveCount(0)
  await expect(page.getByRole("button", { name: /삭제$/ })).toHaveCount(0)
  await expect(page.getByRole("button", { name: /위로$|아래로$/ })).toHaveCount(0)
  await expect(page.getByRole("tab", { name: "프로그램" })).toHaveCount(0)
  // 관리 메뉴 탭에는 관리 메뉴만
  await expect(nameButton(page, "홈")).toHaveCount(0)
  await page.getByRole("tab", { name: "사용자 메뉴" }).click()
  await expect(nameButton(page, "홈")).toBeVisible()
  await expect(nameButton(page, "운영")).toHaveCount(0)
})

test("FR-011 머리 설명에 역할 관리로 가는 링크가 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await expect(page.getByRole("link", { name: "역할 관리", exact: true }).last()).toHaveAttribute(
    "href",
    "/admin/roles",
  )
})

test("US5-AC2 이름을 누르면 오른쪽 칸에서 상위 메뉴·연결 화면을 보고 고쳐 저장한다. 종류 칸은 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await nameButton(page, "코드 관리").click()
  const panel = detail(page, "메뉴 수정")
  await expect(page.getByRole("dialog")).toHaveCount(0)
  await expect(page.getByRole("row", { name: /코드 관리/ })).toHaveAttribute("aria-current", "true")
  await expect(panel.getByLabel("상위 메뉴")).toHaveValue("")
  await expect(panel.getByLabel("연결 화면")).toHaveValue("ADMIN_CODES")
  await expect(panel.getByText("종류")).toHaveCount(0)
  await expect(panel.getByLabel("경로")).toHaveCount(0)
  await panel.getByLabel("이름").fill("코드")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.modify).toMatchObject({ name: "코드", programKey: "ADMIN_CODES", sortOrder: 20 })
})

test("US5-AC2 상위 메뉴를 바꿔 저장하면 새 상위 메뉴의 맨 뒤로 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await nameButton(page, "코드 관리").click()
  const panel = detail(page, "메뉴 수정")
  await panel.getByLabel("상위 메뉴").selectOption({ label: "운영" })
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.modify).toMatchObject({ parentId: 7, programKey: "ADMIN_CODES", sortOrder: 30 })
})

test("FR-009 하위 메뉴가 있는 상위 메뉴는 맨 위·연결 화면 없음으로 고정된다 @wide", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await nameButton(page, "운영").click()
  const panel = detail(page, "메뉴 수정")
  await expect(panel.getByLabel("상위 메뉴")).toBeDisabled()
  await expect(panel.getByLabel("연결 화면")).toBeDisabled()
  await expect(panel.getByText("하위 메뉴가 있어 연결 화면 없이 상위 메뉴로 둬요.")).toBeVisible()
})

test("US5-AC7 오른쪽 칸에서 고치다가 다른 메뉴를 누르면 묻고, 머무르면 고치던 값이 남는다 @wide", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await nameButton(page, "코드 관리").click()
  await detail(page, "메뉴 수정").getByLabel("이름").fill("바뀐 이름")
  await nameButton(page, "메뉴").click()
  const ask = page.getByRole("alertdialog")
  await expect(ask).toBeVisible()
  await ask.getByRole("button", { name: "머무르기" }).click()
  await expect(detail(page, "메뉴 수정").getByLabel("이름")).toHaveValue("바뀐 이름")
})

test("FR-012 저장하지 않은 변경이 있으면 드로어를 닫기 전에 묻는다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await nameButton(page, "코드 관리").click()
  const drawer = page.getByRole("dialog")
  await drawer.getByLabel("이름").fill("바뀐 이름")
  await drawer.getByRole("button", { name: "취소" }).click()
  await expect(page.getByRole("alertdialog")).toBeVisible()
})

test("US5-AC3 키보드로 메뉴를 옮기면 바뀐 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  const handle = page.getByRole("button", { name: "코드 관리 옮기기" })
  await handle.focus()
  // 키를 너무 빨리 누르면 끌기 센서가 집기 전에 다음 키를 받는다. 화면 낭독기 안내(live region)로 단계를 기다린다
  const live = page.locator('[id^="DndLiveRegion"]')
  await page.keyboard.press("Space")
  await expect(live).toContainText("옮기는 중")
  await page.keyboard.press("ArrowUp")
  await expect(live).toContainText("메뉴 자리로")
  await page.keyboard.press("Space")
  // "코드 관리"가 "운영" 묶음의 "메뉴" 앞(두 번째)으로 들어간다
  await expect.poll(() => sent.order).toEqual(expect.arrayContaining([{ id: 3, parentId: 7, sortOrder: 20 }]))
})

test("US5-AC4 둘을 골라 사용 안 함으로 바꾸고, 상위 메뉴로 옮긴다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await page.getByRole("checkbox", { name: "코드 관리 고르기" }).click()
  await page.getByRole("checkbox", { name: "메뉴 고르기" }).click()
  const toolbar = page.getByRole("toolbar", { name: "고른 메뉴 처리" })
  await expect(toolbar.getByText("2개 선택")).toBeVisible()
  await toolbar.getByRole("button", { name: "사용 안 함" }).click()
  await expect.poll(() => sent.enabled).toEqual({ ids: [3, 9], enabled: false })

  await toolbar.getByRole("button", { name: "상위 메뉴로 옮기기" }).click()
  await page.getByRole("menuitem", { name: "설정" }).click()
  await expect
    .poll(() => sent.order)
    .toEqual(
      expect.arrayContaining([
        { id: 9, parentId: 10, sortOrder: 10 },
        { id: 3, parentId: 10, sortOrder: 20 },
      ]),
    )
})

test("US5-AC5 하위 메뉴가 남는 상위 메뉴를 지우려 하면 아무것도 지우지 않고 알린다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await page.getByRole("checkbox", { name: "운영 고르기" }).click()
  await page.getByRole("toolbar", { name: "고른 메뉴 처리" }).getByRole("button", { name: "삭제" }).click()
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제" }).click()
  await expect(page.getByText("하위 메뉴가 남는 상위 메뉴가 있거나", { exact: false })).toBeVisible()
})

test("FR-007 사용자 메뉴 탭에서 만들면 대상 USER로 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await page.getByRole("tab", { name: "사용자 메뉴" }).click()
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const panel = detail(page, "메뉴 추가")
  await expect(panel.getByText("대상: 사용자 메뉴", { exact: false })).toBeVisible()
  await panel.getByLabel("이름").fill("이벤트")
  await panel.getByLabel("연결 화면").selectOption("NOTICES")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent.create)
    .toMatchObject({ name: "이벤트", programKey: "NOTICES", audience: "USER", visibility: "PUBLIC" })
})

test("US4-AC2 관리 메뉴는 관리 화면만 연결 화면으로 고르고, 고르면 경로와 권한이 보이며 고른 상위 메뉴의 맨 뒤에 만든다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const panel = detail(page, "메뉴 추가")
  await expect(panel.getByLabel("경로")).toHaveCount(0)
  await expect(panel.getByLabel("연결 화면").locator("option", { hasText: "공지" })).toHaveCount(0)
  await panel.getByLabel("상위 메뉴").selectOption({ label: "운영" })
  await panel.getByLabel("이름").fill("코드 도구")
  await panel.getByLabel("연결 화면").selectOption("ADMIN_CODES")
  await expect(panel.getByText('경로 /admin/codes · "공통코드 관리" 권한이 있는 사람')).toBeVisible()
  await panel.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent.create)
    .toMatchObject({ name: "코드 도구", programKey: "ADMIN_CODES", audience: "ADMIN", parentId: 7, sortOrder: 30 })
})

test("US4-AC3 로그인이 필요한 사용자 프로그램을 고르면 공개 범위는 '회원만'만 고를 수 있다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await page.getByRole("tab", { name: "사용자 메뉴" }).click()
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const panel = detail(page, "메뉴 추가")
  await panel.getByLabel("이름").fill("내 정보")
  await panel.getByLabel("연결 화면").selectOption("MY_PAGE")
  await expect(panel.getByRole("radio", { name: "비회원 포함 모두" })).toBeDisabled()
  await expect(panel.getByRole("radio", { name: "회원만" })).toBeChecked()
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.create).toMatchObject({ programKey: "MY_PAGE", visibility: "MEMBER" })
})

test("US5-AC6 맨 위에 연결 화면 없이 만들면 상위 메뉴가 되고, 상위 메뉴 아래에 두려면 연결 화면을 골라야 한다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/menus")
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  const panel = detail(page, "메뉴 추가")
  await expect(panel.getByLabel("상위 메뉴")).toHaveValue("")
  await panel.getByLabel("이름").fill("빈 메뉴")
  await panel.getByLabel("상위 메뉴").selectOption({ label: "운영" })
  await panel.getByRole("button", { name: "저장" }).click()
  await expect(panel.getByText("상위 메뉴 아래에 두려면 연결 화면을 골라 주세요.")).toBeVisible()
  expect(sent.create).toBeUndefined()

  await panel.getByLabel("상위 메뉴").selectOption({ label: "없음 (맨 위)" })
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.create).toMatchObject({ name: "빈 메뉴", audience: "ADMIN" })
  expect((sent.create as { programKey?: string }).programKey).toBeUndefined()
  expect((sent.create as { parentId?: number }).parentId).toBeUndefined()
})

test("FR-010 고른 메뉴가 있으면 새 메뉴의 상위 메뉴 처음 값은 그 메뉴의 상위 메뉴다 @wide", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/menus")
  await nameButton(page, "메뉴").click()
  await page.getByRole("button", { name: "메뉴 추가" }).click()
  await expect(detail(page, "메뉴 추가").getByLabel("상위 메뉴")).toHaveValue("7")
})

test("US5-AC8 FR-012 좁은 폭에서는 트리만 보이고 상세가 화면 전체를 덮는 드로어로 열린다", async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 })
  await mock(page)
  await page.goto("/admin/menus")
  await nameButton(page, "코드 관리").click()
  const box = await page.getByRole("dialog").boundingBox()
  expect(box?.width).toBeGreaterThanOrEqual(355)
})
