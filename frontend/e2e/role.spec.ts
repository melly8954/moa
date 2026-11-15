import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 역할 관리 (rbac 헬퍼). 백엔드 대신 API를 가로챈다

// moa: 역할 창은 관리 메뉴 트리로 권한을 고른다 (specs/003-admin-roles FR-004)
const PERMISSION_TREE = {
  menus: [
    {
      menuId: 3,
      name: "코드 관리",
      enabled: true,
      permission: { key: "CODE_MANAGE", name: "공통코드 관리" },
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
          permission: { key: "ROLE_MANAGE", name: "역할 관리" },
          children: [],
        },
      ],
    },
  ],
  unlisted: [
    { programKey: "ADMIN_NOTICES", name: "공지 관리", permission: { key: "NOTICE_MANAGE", name: "공지 관리" } },
  ],
}
// 구성원 탭의 사용자 (moa, specs/003-admin-roles FR-006)
const member = (id: number, loginId: string, role: "USER" | "ADMIN", isMember: boolean) => ({
  id,
  loginId,
  name: loginId,
  role,
  member: isMember,
})
const page_ = <T>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
const ROLES = [
  {
    id: 1,
    code: "MANAGER",
    name: "운영 매니저",
    description: "코드를 관리한다",
    permissions: ["CODE_MANAGE"],
    userCount: 3,
  },
  { id: 2, code: "SECURITY", name: "보안 담당", description: null, permissions: [], userCount: 0 },
]

type Sent = { bulkDelete?: unknown; add?: unknown; remove?: unknown }

async function mock(page: Page, onSave?: (body: unknown) => { status: number; json: unknown }, sent: Sent = {}) {
  await signedIn(page, { permissions: ["ROLE_MANAGE"] })
  await page.route("**/api/v1/admin/roles/permission-tree", (route) => route.fulfill({ json: PERMISSION_TREE }))
  await page.route("**/api/v1/admin/roles", (route) => {
    if (route.request().method() === "POST") {
      const reply = onSave?.(route.request().postDataJSON()) ?? { status: 201, json: ROLES[0] }
      return route.fulfill(reply)
    }
    return route.fulfill({ json: ROLES })
  })
  await page.route("**/api/v1/admin/roles/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  // 구성원 탭: 검색어가 없으면 구성원만, 있으면 찾은 사람 모두와 구성원 여부
  await page.route("**/api/v1/admin/roles/1/members**", (route) => {
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
              member(10, "operator", "USER", true),
              member(11, "operator2", "USER", false),
              member(2, "admin", "ADMIN", false),
            ]
          : [member(10, "operator", "USER", true)],
      ),
    })
  })
}

test("역할 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("cell", { name: "운영 매니저", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US2-AC1 역할 추가: 관리 메뉴 트리를 체크하면 그 메뉴의 권한을 보내고, 저장하면 구성원 탭이 열린다 (키 입력 없음)", async ({
  page,
}) => {
  let sent: unknown
  await mock(page, (body) => {
    sent = body
    return { status: 201, json: ROLES[0] }
  })
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const drawer = page.getByRole("dialog")
  // 새 역할은 저장 전에 구성원 탭이 없다
  await expect(drawer.getByRole("tab", { name: "구성원" })).toHaveCount(0)
  await drawer.getByLabel("역할 코드").fill("VIEWER")
  await drawer.getByLabel("이름").fill("조회 담당")
  await expect(drawer.getByText("메뉴에 없는 프로그램")).toBeVisible()
  await expect(drawer.getByText("꺼짐")).toBeVisible()
  await drawer.getByRole("checkbox", { name: /^코드 관리/ }).click()
  // 묶음 메뉴를 체크하면 하위 메뉴가 모두 체크된다
  await drawer.getByRole("checkbox", { name: /^시스템/ }).click()
  await expect(drawer.getByRole("checkbox", { name: /^역할/ })).toBeChecked()
  await expectNoSeriousA11yViolations(page)
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent)
    .toEqual({ code: "VIEWER", name: "조회 담당", permissions: ["CODE_MANAGE", "ROLE_MANAGE"] })
  await expect(page.getByRole("dialog").getByRole("tab", { name: "구성원", selected: true })).toBeVisible()
})

test("내 권한 밖이면(A005) 그렇게 알린다", async ({ page }) => {
  await mock(page, () => ({ status: 403, json: { code: "A005", message: "", timestamp: "", path: "" } }))
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const drawer = page.getByRole("dialog")
  await drawer.getByLabel("역할 코드").fill("SUPER")
  await drawer.getByLabel("이름").fill("최고")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect(drawer.getByText("내가 가진 권한 밖의 권한은 다룰 수 없어요.")).toBeVisible()
})

test("US3-AC1·AC2 역할 목록은 탭과 행 단추가 없고, 검색 칸으로 화면에서 거른다 @wide", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("tab")).toHaveCount(0)
  await expect(page.getByRole("button", { name: /삭제$|수정$|구성원$/ })).toHaveCount(0)
  await expect(page.getByRole("button", { name: "보안 담당", exact: true })).toBeVisible()
  await page.getByLabel("역할 찾기").fill("매니저")
  await expect(page.getByRole("button", { name: "운영 매니저", exact: true })).toBeVisible()
  await expect(page.getByRole("button", { name: "보안 담당", exact: true })).toHaveCount(0)
})

test("US3-AC3 둘을 골라 지우면 사용자 수를 알리고 되물은 뒤 한꺼번에 보낸다", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await page.getByRole("checkbox", { name: "모두 고르기" }).click()
  await page.getByRole("toolbar", { name: "고른 역할 처리" }).getByRole("button", { name: "삭제" }).click()
  await expect(page.getByRole("alertdialog")).toContainText("3명")
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제" }).click()
  await expect.poll(() => sent.bulkDelete).toEqual({ ids: [1, 2] })
})

test("FR-009 머리 설명에 메뉴 관리로 가는 링크가 있다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("link", { name: "메뉴 관리", exact: true }).last()).toHaveAttribute(
    "href",
    "/admin/menus",
  )
})

test("US4-AC1·AC2·AC4 구성원 탭: 검색하면 구성원 여부가 보이고, 체크해 한꺼번에 더하며, 관리자는 체크할 수 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "운영 매니저", exact: true }).click()
  const drawer = page.getByRole("dialog")
  await drawer.getByRole("tab", { name: "구성원" }).click()
  // 검색하지 않으면 구성원만
  await expect(drawer.getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
  await expect(drawer.getByRole("cell", { name: "operator2", exact: true })).toHaveCount(0)

  await drawer.getByLabel("사용자 찾기").fill("op")
  await drawer.getByRole("button", { name: "찾기" }).click()
  await expect(drawer.getByRole("cell", { name: "operator2", exact: true }).first()).toBeVisible()
  await expect(drawer.getByRole("checkbox", { name: "admin 고르기" })).toBeDisabled()
  await expect(drawer.getByRole("cell", { name: "모든 권한" })).toBeVisible()
  await expectNoSeriousA11yViolations(page)

  await drawer.getByRole("checkbox", { name: "operator2 고르기" }).click()
  await drawer.getByRole("toolbar", { name: "고른 사람 처리" }).getByRole("button", { name: "더하기" }).click()
  await expect.poll(() => sent.add).toEqual({ userIds: [11] })
})

test("US4-AC3 구성원을 골라 한꺼번에 뺀다", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, undefined, sent)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "운영 매니저", exact: true }).click()
  const drawer = page.getByRole("dialog")
  await drawer.getByRole("tab", { name: "구성원" }).click()
  await drawer.getByRole("checkbox", { name: "operator 고르기" }).click()
  await drawer.getByRole("toolbar", { name: "고른 사람 처리" }).getByRole("button", { name: "빼기" }).click()
  await expect.poll(() => sent.remove).toEqual({ userIds: [10] })
})
