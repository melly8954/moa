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
const user = (
  id: number,
  loginId: string,
  role: "USER" | "ADMIN",
  accessRoles: { id: number; code: string; name: string }[] = [],
) => ({
  id,
  loginId,
  name: loginId,
  role,
  accessRoles,
})
const page = <T>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
const ROLES = [
  {
    id: 1,
    code: "MANAGER",
    name: "운영 매니저",
    description: "코드를 관리한다",
    permissions: ["CODE_MANAGE"],
    userCount: 3,
  },
]

async function mock(page: Page, onSave?: (body: unknown) => { status: number; json: unknown }) {
  await signedIn(page, { permissions: ["ROLE_MANAGE"] })
  await page.route("**/api/v1/admin/roles/permission-tree", (route) => route.fulfill({ json: PERMISSION_TREE }))
  await page.route("**/api/v1/admin/roles", (route) => {
    if (route.request().method() === "POST") {
      const reply = onSave?.(route.request().postDataJSON()) ?? { status: 201, json: ROLES[0] }
      return route.fulfill(reply)
    }
    return route.fulfill({ json: ROLES })
  })
}

test("역할 관리: 가로 넘침·접근성", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await expect(page.getByRole("cell", { name: "운영 매니저", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US2-AC1 역할 추가: 관리 메뉴 트리를 체크하면 그 메뉴의 권한을 보낸다 (키 입력 없음)", async ({ page }) => {
  let sent: unknown
  await mock(page, (body) => {
    sent = body
    return { status: 201, json: ROLES[0] }
  })
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("역할 코드").fill("VIEWER")
  await dialog.getByLabel("이름").fill("조회 담당")
  await expect(dialog.getByText("메뉴에 없는 프로그램")).toBeVisible()
  await expect(dialog.getByText("꺼짐")).toBeVisible()
  await dialog.getByRole("checkbox", { name: /^코드 관리/ }).click()
  // 묶음 메뉴를 체크하면 하위 메뉴가 모두 체크된다
  await dialog.getByRole("checkbox", { name: /^시스템/ }).click()
  await expect(dialog.getByRole("checkbox", { name: /^역할/ })).toBeChecked()
  await expectNoSeriousA11yViolations(page)
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog).toBeHidden()
  expect(sent).toEqual({ code: "VIEWER", name: "조회 담당", permissions: ["CODE_MANAGE", "ROLE_MANAGE"] })
})

test("내 권한 밖이면(A005) 그렇게 알린다", async ({ page }) => {
  await mock(page, () => ({ status: 403, json: { code: "A005", message: "", timestamp: "", path: "" } }))
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "역할 추가" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("역할 코드").fill("SUPER")
  await dialog.getByLabel("이름").fill("최고")
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog.getByText("내가 가진 권한 밖의 권한은 다룰 수 없어요.")).toBeVisible()
})

test("삭제는 사용자 수를 알리고 되묻는다", async ({ page }) => {
  await mock(page)
  await page.goto("/admin/roles")
  await page.getByRole("button", { name: "운영 매니저 삭제" }).click()
  await expect(page.getByRole("alertdialog")).toContainText("3명")
  await page.getByRole("button", { name: "취소" }).click()
})

test("US3-AC1 사용자별 역할 탭: 관리자는 '모든 권한', 운영자는 받은 역할이 보이고 그 자리에서 바꾼다 @wide", async ({
  page: p,
}) => {
  let sent: unknown
  await mock(p)
  await p.route("**/api/v1/admin/roles/users**", (route) =>
    route.fulfill({
      json: page([
        user(2, "admin", "ADMIN"),
        user(10, "operator", "USER", [{ id: 1, code: "MANAGER", name: "운영 매니저" }]),
      ]),
    }),
  )
  await p.route("**/api/v1/admin/users/10/roles", (route) => {
    sent = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await p.goto("/admin/roles")
  await p.getByRole("tab", { name: "사용자별 역할" }).click()
  await expect(p.getByRole("cell", { name: "모든 권한", exact: true })).toBeVisible()
  await expect(p.getByRole("button", { name: "admin 역할 변경" })).toHaveCount(0)
  await expect(p.getByRole("cell", { name: "운영 매니저", exact: true })).toBeVisible()
  await expectNoSeriousA11yViolations(p)

  await p.getByRole("button", { name: "operator 역할 변경" }).click()
  const dialog = p.getByRole("dialog")
  await dialog.getByRole("checkbox", { name: /운영 매니저/ }).click()
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent).toEqual({ roleIds: [] })
})

test("US4-AC2 구성원: 역할의 구성원을 보고, 사용자를 찾아 더한다 @wide", async ({ page: p }) => {
  let added: unknown
  await mock(p)
  await p.route("**/api/v1/admin/roles/1/members", (route) => {
    if (route.request().method() === "POST") {
      added = route.request().postDataJSON()
      return route.fulfill({ status: 204 })
    }
    return route.fulfill({ json: [user(10, "operator", "USER")] })
  })
  await p.route("**/api/v1/admin/roles/users**", (route) =>
    route.fulfill({
      json: page([user(10, "operator", "USER"), user(11, "operator2", "USER"), user(2, "admin", "ADMIN")]),
    }),
  )
  await p.goto("/admin/roles")
  await p.getByRole("button", { name: "운영 매니저 구성원" }).click()
  const dialog = p.getByRole("dialog")
  await expect(dialog.getByRole("cell", { name: "operator", exact: true }).first()).toBeVisible()
  await dialog.getByLabel("사용자 찾기").fill("operator")
  await dialog.getByRole("button", { name: "찾기" }).click()
  await expect(dialog.getByText("이미 구성원")).toBeVisible()
  await expect(dialog.getByRole("button", { name: "admin 더하기" })).toHaveCount(0)
  await dialog.getByRole("button", { name: "operator2 더하기" }).click()
  await expect.poll(() => added).toEqual({ userId: 11 })
})
