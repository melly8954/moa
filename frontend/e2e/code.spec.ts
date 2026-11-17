import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 공통코드 (code 헬퍼). 백엔드 대신 API를 가로챈다
// moa: 그룹은 코드에 등록하고 화면은 그룹 > 항목 트리 표다 (specs/004-admin-codes FR-004~FR-007, US1, research R8)

const TREE = [
  {
    groupCode: "GAME_GENRE",
    name: "게임 장르",
    description: null,
    usedBy: "게임 등록 화면",
    codes: [
      { code: "001", name: "FPS", sortOrder: 0, enabled: true },
      { code: "002", name: "RPG", sortOrder: 1, enabled: true },
      { code: "003", name: "퍼즐", sortOrder: 2, enabled: false },
    ],
  },
  {
    groupCode: "REPORT_REASON",
    name: "신고 사유",
    description: null,
    usedBy: "신고 화면",
    codes: [{ code: "001", name: "욕설", sortOrder: 0, enabled: true }],
  },
]

type Sent = { create?: unknown; modify?: unknown; order?: unknown; enabled?: unknown; bulkDelete?: unknown }

async function mockCodes(page: Page, sent: Sent = {}, tree: unknown[] = TREE) {
  await signedIn(page, { permissions: ["CODE_MANAGE"] })
  await page.route("**/api/v1/admin/code-groups/tree", (route) => route.fulfill({ json: tree }))
  await page.route("**/api/v1/admin/code-groups/*/codes", (route) => {
    sent.create = route.request().postDataJSON()
    const body = sent.create as { code?: string; name: string }
    return body.code === "001"
      ? route.fulfill({ status: 409, json: { code: "R002", message: "", timestamp: "", path: "" } })
      : route.fulfill({ status: 201, json: { code: "004", name: body.name, sortOrder: 3, enabled: true } })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/order", (route) => {
    sent.order = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/enabled", (route) => {
    sent.enabled = { url: route.request().url(), body: route.request().postDataJSON() }
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/0*", (route) => {
    sent.modify = route.request().postDataJSON()
    return route.fulfill({ json: { code: "001", name: "FPS", sortOrder: 0, enabled: false } })
  })
}

test("공통코드: 가로 넘침·접근성", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByRole("button", { name: "FPS", exact: true })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US1-AC1 그룹 행 아래 항목이 들여쓰기된 트리 표이고, 그룹을 더하는 단추는 없다 (FR-007 머리 안내)", async ({
  page,
}) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByRole("heading", { name: "공통코드" })).toBeVisible()
  await expect(page.getByText("그룹은 그 값을 쓰는 기능이 등록해요.")).toBeVisible()
  await expect(page.getByText("GAME_GENRE")).toBeVisible()
  await expect(page.getByText("쓰는 곳: 게임 등록 화면")).toBeVisible()
  await expect(page.getByRole("row", { name: /퍼즐/ })).toContainText("사용 안 함")
  await expect(page.getByRole("button", { name: /그룹 추가/ })).toHaveCount(0)
  await expect(page.getByRole("checkbox", { name: "게임 장르 고르기" })).toHaveCount(0)

  // 그룹 행은 접고 편다
  await page.getByRole("button", { name: "게임 장르 접기" }).click()
  await expect(page.getByRole("button", { name: "FPS", exact: true })).toHaveCount(0)
  await page.getByRole("button", { name: "게임 장르 펼치기" }).click()
  await expect(page.getByRole("button", { name: "FPS", exact: true })).toBeVisible()
})

test("US1-AC2 그룹의 항목 추가는 드로어에서 이름만 적으면 코드를 비워 보낸다. 겹치는 코드는 코드 칸에 알린다", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "게임 장르에 항목 추가" }).click()
  const drawer = page.getByRole("dialog")
  await expect(drawer.getByText("그룹: 게임 장르")).toBeVisible()
  await drawer.getByLabel("이름").fill("퍼즐 2")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.create).toEqual({ name: "퍼즐 2" })
  await expect(drawer).toBeHidden()

  await page.getByRole("button", { name: "게임 장르에 항목 추가" }).click()
  await drawer.getByLabel("이름").fill("겹침")
  await drawer.getByLabel("코드").fill("abc")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect(drawer.getByText("영문 대문자·숫자·밑줄(_)로 적어 주세요.")).toBeVisible()
  await drawer.getByLabel("코드").fill("001")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect(drawer.getByText("이미 있는 코드예요.")).toBeVisible()
})

test("US1-AC3 항목 이름을 누르면 드로어에서 이름과 사용을 고치고 코드는 보기만 한다", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "FPS", exact: true }).click()
  const drawer = page.getByRole("dialog")
  await expect(drawer.getByLabel("코드")).toHaveValue("001")
  await expect(drawer.getByLabel("코드")).toHaveAttribute("readonly", "")
  await drawer.getByRole("switch", { name: "사용" }).click()
  await expectNoSeriousA11yViolations(page)
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.modify).toEqual({ name: "FPS", enabled: false })
})

test("US1-AC4 항목 둘을 골라 사용 안 함으로 바꾸고, 다른 그룹 것을 함께 고르면 한 그룹씩 처리하라고 안내한다", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("checkbox", { name: "FPS 고르기" }).click()
  await page.getByRole("checkbox", { name: "RPG 고르기" }).click()
  const toolbar = page.getByRole("toolbar", { name: "고른 항목 처리" })
  await expect(toolbar.getByText("2개 선택")).toBeVisible()
  await toolbar.getByRole("button", { name: "사용 안 함" }).click()
  await expect
    .poll(() => sent.enabled)
    .toEqual({
      url: expect.stringContaining("/GAME_GENRE/codes/enabled"),
      body: { codes: ["001", "002"], enabled: false },
    })

  await page.getByRole("checkbox", { name: "욕설 고르기" }).click()
  await expect(toolbar.getByText("한 그룹씩 처리해요")).toBeVisible()
  await expect(toolbar.getByRole("button", { name: "사용 안 함" })).toHaveCount(0)
})

test("US1-AC4 고른 항목을 지우기 전에 되묻는다", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("checkbox", { name: "퍼즐 고르기" }).click()
  await page.getByRole("toolbar", { name: "고른 항목 처리" }).getByRole("button", { name: "삭제" }).click()
  const ask = page.getByRole("alertdialog", { name: "항목 1개를 지울까요?" })
  await ask.getByRole("button", { name: "삭제" }).click()
  await expect.poll(() => sent.bulkDelete).toEqual({ codes: ["003"] })
})

test("US1-AC5 키보드로 항목을 옮기면 그 그룹의 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  const handle = page.getByRole("button", { name: "RPG 옮기기" })
  await handle.focus()
  // 키를 너무 빨리 누르면 끌기 센서가 집기 전에 다음 키를 받는다. 화면 낭독기 안내(live region)로 단계를 기다린다
  const live = page.locator('[id^="DndLiveRegion"]')
  await page.keyboard.press("Space")
  await expect(live).toContainText("옮기는 중")
  await page.keyboard.press("ArrowUp")
  await expect(live).toContainText("FPS 자리로")
  await page.keyboard.press("Space")
  await expect
    .poll(() => sent.order)
    .toEqual([
      { code: "002", sortOrder: 0 },
      { code: "001", sortOrder: 1 },
      { code: "003", sortOrder: 2 },
    ])
})

test("US1-AC6 등록된 그룹이 없으면 그룹을 등록하는 곳을 안내한다", async ({ page }) => {
  await mockCodes(page, {}, [])
  await page.goto("/admin/codes")
  await expect(page.getByText("공통코드를 쓰는 기능이 생기면 그 기능이 그룹을 등록해요.")).toBeVisible()
})
