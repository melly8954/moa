import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 권한 관리 (rbac 헬퍼, moa 이름 "권한 관리"). 백엔드 대신 API를 가로챈다
// moa (specs/003-admin-roles FR-005·FR-010·FR-011, research R21, 2026-11-21): 왼쪽 역할 목록 · 오른쪽 상세 칸(region)의 두 칸이다.
//   좁은 폭은 목록 + 드로어(dialog). 최고 관리자 역할은 맨 위에 고정 역할로 있고, 쓸 메뉴와 구성원이 한 칸에 함께 보인다(탭 없음)

// moa: 역할은 관리 메뉴 트리로 권한을 고른다 (specs/003-admin-roles FR-004)
const PERMISSION_TREE = {
  menus: [
    {
      menuId: 3,
      name: "코드 관리",
      enabled: true,
      permission: { key: "CODE_MANAGE", name: "코드 관리" },
      children: [],
    },
    {
      menuId: 9,
      name: "시스템",
      enabled: true,
      permission: null,
      children: [
        {
          menuId: 5,
          name: "역할",
          enabled: false,
          permission: { key: "ROLE_MANAGE", name: "권한 관리" },
          children: [],
        },
      ],
    },
  ],
  unlisted: [
    { screenKey: "ADMIN_NOTICES", name: "공지 관리", permission: { key: "NOTICE_MANAGE", name: "공지 관리" } },
  ],
}
// 구성원의 한 줄 (moa, specs/003-admin-roles FR-006). 관리자 계정만 오고, superAdmin은 최고 관리자 역할을 가졌나
const member = (id: number, loginId: string, superAdmin: boolean, isMember: boolean) => ({
  id,
  loginId,
  name: loginId,
  superAdmin,
  member: isMember,
})
const page_ = <T>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
const role = (over: Record<string, unknown>) => ({
  description: null,
  permissions: [],
  userCount: 0,
  fixed: false,
  permissionNames: [],
  grantable: true,
  blockReason: null,
  ...over,
})
const ROLES = [
  role({
    id: 9,
    code: "SUPER_ADMIN",
    name: "최고 관리자",
    description: "모든 관리 메뉴와 기능을 써요",
    userCount: 1,
    fixed: true,
  }),
  role({
    id: 1,
    code: "MANAGER",
    name: "운영 매니저",
    description: "코드를 관리한다",
    permissions: ["CODE_MANAGE"],
    permissionNames: ["코드 관리"],
    userCount: 3,
  }),
  role({ id: 2, code: "SECURITY", name: "보안 담당" }),
]

type Sent = { bulkDelete?: unknown; add?: unknown; remove?: unknown }

async function mock(page: Page, onSave?: (body: unknown) => { status: number; json: unknown }, sent: Sent = {}) {
  await signedIn(page, { permissions: ["ROLE_MANAGE"] })
  await page.route("**/api/v1/admin/roles/permission-tree", (route) => route.fulfill({ json: PERMISSION_TREE }))
  await page.route("**/api/v1/admin/roles", (route) => {
    if (route.request().method() === "POST") {
      const reply = onSave?.(route.request().postDataJSON()) ?? { status: 201, json: ROLES[1] }
      return route.fulfill(reply)
    }
    return route.fulfill({ json: ROLES })
  })
  await page.route("**/api/v1/admin/roles/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  // 구성원: 검색어가 없으면 구성원만, 있으면 찾은 관리자 계정 모두와 구성원 여부
  await page.route("**/api/v1/admin/roles/*/members**", (route) => {
    const request = route.request()
    if (request.method() === "POST") {
      if (request.url().endsWith("/remove")) sent.remove = request.postDataJSON()
      else sent.add = request.postDataJSON()
      return route.fulfill({ status: 204 })
    }
    const keyword = new URL(request.url()).searchParams.get("keyword")
    return route.fulfill({
      json: page_(
        keyword
          ? [
              member(10, "operator", false, true),
              member(11, "operator2", false, false),
              member(2, "admin", true, false),
            ]
          : [member(10, "operator", false, true)],
      ),
    })
  })
}

// 넓은 폭의 오른쪽 상세 칸. 이름은 역할 이름(새 역할은 "역할 추가")이다
const detail = (page: Page, name: string) => page.getByRole("region", { name })
const roleButton = (page: Page, name: string) => page.getByRole("button", { name, exact: true })

test("권한 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(roleButton(page, "운영 매니저")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US4-AC1·AC2 왼쪽은 최고 관리자가 맨 위인 역할 목록(이름·구성원 수, 행 단추·탭 없음), 오른쪽은 고르라는 안내이고, 검색 칸으로 거른다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("heading", { name: "권한 관리" })).toBeVisible()
  await expect(page.getByRole("tab")).toHaveCount(0)
  // 행에는 단추가 없다 (일괄 줄의 "삭제"는 표 밖이다)
  await expect(page.getByRole("row").getByRole("button", { name: /삭제$|수정$|구성원$/ })).toHaveCount(0)
  await expect(page.getByRole("row").nth(1)).toContainText("최고 관리자")
  await expect(page.getByRole("checkbox", { name: "최고 관리자 고르기" })).toBeDisabled()
  await expect(page.getByText("왼쪽에서 역할을 고르거나 역할을 추가해요.")).toBeVisible()
  await page.getByLabel("역할 찾기").fill("매니저")
  await expect(roleButton(page, "운영 매니저")).toBeVisible()
  await expect(roleButton(page, "보안 담당")).toHaveCount(0)
})

test("US4-AC3 역할 행을 누르면 오른쪽 칸에 이름·코드와 쓸 메뉴·구성원이 탭 없이 함께 보인다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/roles")
  await roleButton(page, "운영 매니저").click()
  const panel = detail(page, "운영 매니저")
  await expect(panel).toContainText("MANAGER")
  await expect(panel).toContainText("구성원 3명")
  await expect(panel.getByText("쓸 메뉴", { exact: true })).toBeVisible()
  await expect(panel.getByRole("heading", { name: "구성원" })).toBeVisible()
  await expect(panel.getByRole("checkbox", { name: /^코드 관리/ })).toBeChecked()
  await expect(panel.getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
  await expect(panel.getByRole("tab")).toHaveCount(0)
  await expect(page.getByRole("row", { name: /운영 매니저/ })).toHaveAttribute("aria-current", "true")
  await expectNoSeriousA11yViolations(page)
})

test("US3-AC1 역할 추가: 관리 메뉴 트리를 체크하면 그 메뉴의 권한을 보내고, 저장하면 그 역할의 구성원을 더할 수 있다 (키 입력 없음) @wide", async ({
  page,
}) => {
  let sent: unknown
  await mock(page, (body) => {
    sent = body
    return { status: 201, json: ROLES[1] }
  })
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const panel = detail(page, "역할 추가")
  // 새 역할은 저장 전에 구성원을 더할 수 없다
  await expect(panel.getByText("저장하면 구성원을 더할 수 있어요.")).toBeVisible()
  await panel.getByLabel("역할 코드").fill("VIEWER")
  await panel.getByLabel("이름").fill("조회 담당")
  await expect(panel.getByText("메뉴에 없는 화면")).toBeVisible()
  await expect(panel.getByText("꺼짐")).toBeVisible()
  await panel.getByRole("checkbox", { name: /^코드 관리/ }).click()
  // 상위 메뉴를 체크하면 하위 메뉴가 모두 체크된다
  await panel.getByRole("checkbox", { name: /^시스템/ }).click()
  await expect(panel.getByRole("checkbox", { name: /^역할/ })).toBeChecked()
  await expectNoSeriousA11yViolations(page)
  await panel.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent)
    .toEqual({ code: "VIEWER", name: "조회 담당", permissions: ["CODE_MANAGE", "ROLE_MANAGE"] })
  await expect(detail(page, "운영 매니저").getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
})

test("내 권한 밖이면(A005) 그렇게 알린다 @wide", async ({ page }) => {
  await mock(page, () => ({ status: 403, json: { code: "A005", message: "", timestamp: "", path: "" } }))
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const panel = detail(page, "역할 추가")
  await panel.getByLabel("역할 코드").fill("SUPER")
  await panel.getByLabel("이름").fill("최고")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect(panel.getByText("내가 가진 권한 밖의 권한이나 사용자 관리 권한은 다룰 수 없어요.")).toBeVisible()
})

test("US2-AC2 최고 관리자 역할은 쓸 메뉴가 모두 체크된 채 꺼져 있고 이름·설명을 바꾸거나 저장할 수 없다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: /최고 관리자$/ }).click()
  const panel = detail(page, "최고 관리자")
  await expect(panel.getByText(/바꾸거나 지울 수 없고/)).toBeVisible()
  await expect(panel.getByRole("checkbox", { name: /^코드 관리/ })).toBeChecked()
  await expect(panel.getByRole("checkbox", { name: /^코드 관리/ })).toBeDisabled()
  await expect(panel.getByRole("checkbox", { name: /^공지 관리/ })).toBeChecked()
  await expect(panel.getByLabel("이름")).toBeDisabled()
  await expect(panel.getByRole("button", { name: "저장" })).toHaveCount(0)
})

// 2026-11-21 한 명 고정 (specs/003-admin-roles FR-006·FR-010, research R24·R25)
test("US2-AC4 최고 관리자 역할의 구성원은 보기만 하고 검색·체크·더하기가 없으며 개발자에게 요청하라는 안내가 보인다 @wide", async ({
  page,
}) => {
  await mock(page)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: /최고 관리자$/ }).click()
  const panel = detail(page, "최고 관리자")
  await expect(panel.getByText("operator").first()).toBeVisible()
  await expect(panel.getByText("최고 관리자는 한 명으로 고정이에요. 바꾸려면 개발자에게 요청해요.")).toBeVisible()
  await expect(panel.getByLabel("관리자 계정 찾기")).toHaveCount(0)
  await expect(panel.getByRole("checkbox", { name: /고르기$/ })).toHaveCount(0)
  await expect(panel.getByRole("button", { name: "더하기" })).toHaveCount(0)
})

test("US4-AC4 둘을 골라 지우면 사용자 수를 알리고 되물은 뒤 한꺼번에 보낸다. 최고 관리자 역할은 고를 수 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await page.getByRole("checkbox", { name: "모두 고르기" }).click()
  await page.getByRole("toolbar", { name: "고른 역할 처리" }).getByRole("button", { name: "삭제" }).click()
  await expect(page.getByRole("alertdialog")).toContainText("3명")
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제" }).click()
  await expect.poll(() => sent.bulkDelete).toEqual({ ids: [1, 2] })
})

test("FR-009 머리 설명에 최고 관리자 안내와 메뉴 관리로 가는 링크가 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByText(/최고 관리자 역할은 모든 관리 메뉴를 써요/).first()).toBeVisible()
  await expect(page.getByRole("link", { name: "메뉴 관리", exact: true }).last()).toHaveAttribute(
    "href",
    "/admin/menus",
  )
})

test("US5-AC1·AC2·AC4 구성원: 검색하면 구성원 여부가 보이고, 체크해 한꺼번에 더하며, 최고 관리자는 체크할 수 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await roleButton(page, "운영 매니저").click()
  const panel = detail(page, "운영 매니저")
  // 검색하지 않으면 구성원만
  await expect(panel.getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
  await expect(panel.getByRole("cell", { name: "operator2", exact: true })).toHaveCount(0)

  await panel.getByLabel("관리자 계정 찾기").fill("op")
  await panel.getByRole("button", { name: "찾기" }).click()
  await expect(panel.getByRole("cell", { name: "operator2", exact: true }).first()).toBeVisible()
  await expect(panel.getByRole("checkbox", { name: "admin 고르기" })).toBeDisabled()
  await expect(panel.getByRole("cell", { name: "모든 권한" })).toBeVisible()
  await expectNoSeriousA11yViolations(page)

  await panel.getByRole("checkbox", { name: "operator2 고르기" }).click()
  await panel.getByRole("toolbar", { name: "고른 사람 처리" }).getByRole("button", { name: "더하기" }).click()
  await expect.poll(() => sent.add).toEqual({ userIds: [11] })
})

test("US5-AC3 구성원을 골라 한꺼번에 뺀다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await roleButton(page, "운영 매니저").click()
  const panel = detail(page, "운영 매니저")
  await panel.getByRole("checkbox", { name: "operator 고르기" }).click()
  await panel.getByRole("toolbar", { name: "고른 사람 처리" }).getByRole("button", { name: "빼기" }).click()
  await expect.poll(() => sent.remove).toEqual({ userIds: [10] })
})

test("US4-AC5 1024px보다 좁으면 목록만 보이고 상세는 드로어로 열린다 @wide", async ({ page }) => {
  await page.setViewportSize({ width: 900, height: 800 })
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByText("왼쪽에서 역할을 고르거나 역할을 추가해요.")).toHaveCount(0)
  await roleButton(page, "운영 매니저").click()
  await expect(page.getByRole("dialog").getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
})
