import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { keyboardMoveUp } from "./support/drag"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 공통코드 (code 헬퍼). 백엔드 대신 API를 가로챈다
// moa: 왼쪽 그룹 목록 · 오른쪽 고른 그룹의 머리와 항목 표다. 그룹 추가·설정은 드로어, 항목은 표 안에서 다룬다.
// 그룹과 항목은 지우지 않고 사용 여부로 끈다. 항목 코드는 직접 적는다 (specs/004-admin-codes FR-004~FR-009, US1, 2026-11-21)

const TREE = [
  {
    groupCode: "GAME_GENRE",
    name: "게임 장르",
    description: "게임 등록 화면의 장르",
    enabled: true,
    codes: [
      { code: "FPS", name: "FPS", sortOrder: 0, enabled: true },
      { code: "RPG", name: "RPG", sortOrder: 1, enabled: true },
      { code: "PUZZLE", name: "퍼즐", sortOrder: 2, enabled: false },
    ],
  },
  {
    groupCode: "REPORT_REASON",
    name: "신고 사유",
    description: null,
    enabled: false,
    codes: [{ code: "ABUSE", name: "욕설", sortOrder: 0, enabled: true }],
  },
  { groupCode: "EMPTY_GROUP", name: "빈 그룹", description: null, enabled: true, codes: [] },
]

type Sent = {
  create?: { url: string; body: unknown }
  modify?: { url: string; body: unknown }
  order?: unknown
  enabled?: { url: string; body: unknown }
  orderGroups?: unknown
  createGroup?: unknown
  modifyGroup?: unknown
}

const conflict = { status: 409, json: { code: "R002", message: "", timestamp: "", path: "" } }

async function mockCodes(page: Page, sent: Sent = {}, tree: unknown[] = TREE) {
  await signedIn(page, { permissions: ["CODE_MANAGE"] })
  await page.route("**/api/v1/admin/code-groups/tree", (route) => route.fulfill({ json: tree }))
  await page.route("**/api/v1/admin/code-groups", (route) => {
    sent.createGroup = route.request().postDataJSON()
    const body = sent.createGroup as { groupCode: string; name: string }
    return body.groupCode === "GAME_GENRE"
      ? route.fulfill(conflict)
      : route.fulfill({ status: 201, json: { ...body, enabled: true } })
  })
  for (const groupCode of ["GAME_GENRE", "REPORT_REASON"])
    await page.route(`**/api/v1/admin/code-groups/${groupCode}`, (route) => {
      sent.modifyGroup = route.request().postDataJSON()
      return route.fulfill({ json: { groupCode, ...(sent.modifyGroup as object) } })
    })
  await page.route("**/api/v1/admin/code-groups/*/codes", (route) => {
    const body = route.request().postDataJSON() as { code: string; name: string }
    sent.create = { url: route.request().url(), body }
    return body.code === "FPS"
      ? route.fulfill(conflict)
      : route.fulfill({ status: 201, json: { ...body, sortOrder: 3, enabled: true } })
  })
  // 항목 고치기(이름·사용). 뒤에 등록한 order·enabled가 먼저 잡는다
  await page.route("**/api/v1/admin/code-groups/*/codes/*", (route) => {
    sent.modify = { url: route.request().url(), body: route.request().postDataJSON() }
    return route.fulfill({ json: { code: "X", name: "X", sortOrder: 0, enabled: true } })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/order", (route) => {
    sent.order = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/*/codes/enabled", (route) => {
    sent.enabled = { url: route.request().url(), body: route.request().postDataJSON() }
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/order", (route) => {
    sent.orderGroups = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
}

// 그룹을 고르는 단추. 같은 줄의 끌기 손잡이("… 옮기기")는 글자가 없어 hasText로 거른다
const groupButton = (page: Page, name: string) =>
  page.getByRole("list", { name: "코드 그룹 목록" }).getByRole("button").filter({ hasText: name })

test("공통코드: 가로 넘침·접근성", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByRole("button", { name: "FPS 이름 고치기" })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("US1-AC1 왼쪽 그룹 목록에서 첫 그룹이 골라져 있고, 오른쪽에 그 그룹의 머리와 항목 표가 보인다 (FR-007 머리 안내) @wide", async ({
  page,
}) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByRole("heading", { name: "코드 관리" })).toBeVisible()
  await expect(page.getByText("지우지 않고 사용 안 함으로 꺼요.")).toBeVisible()
  // 그룹 목록: 이름, 그룹 코드, 항목 수. 첫 그룹이 골라져 있다
  await expect(groupButton(page, "게임 장르")).toHaveAttribute("aria-current", "true")
  await expect(groupButton(page, "게임 장르")).toContainText("GAME_GENRE")
  await expect(groupButton(page, "게임 장르")).toContainText("3개")
  await expect(page.getByRole("button", { name: "빈 그룹 옮기기" })).toBeVisible()
  // 그룹 머리와 항목 표
  await expect(page.getByRole("heading", { name: "게임 장르" })).toBeVisible()
  await expect(page.getByText("게임 등록 화면의 장르")).toBeVisible()
  await expect(page.getByRole("row", { name: /퍼즐/ }).getByRole("switch", { name: "퍼즐 사용" })).not.toBeChecked()
  await expect(page.getByRole("row", { name: /PUZZLE/ })).toBeVisible()
  // 삭제는 어디에도 없다 (FR-008)
  await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0)

  const toolbar = page.getByRole("toolbar", { name: "고른 항목 처리" })
  await expect(toolbar.getByText("체크해서 한꺼번에 처리해요.")).toBeVisible()
  await expect(toolbar.getByRole("button", { name: "사용 안 함" })).toBeDisabled()
  const tableTop = async () => (await page.getByRole("table").boundingBox())?.y
  const before = await tableTop()
  await page.getByRole("checkbox", { name: "FPS 고르기" }).click()
  await expect(toolbar.getByText("1개 선택")).toBeVisible()
  expect(await tableTop()).toBe(before)
})

test("US1-AC2 추가 줄에 코드와 이름을 적고 엔터로 만든다. 코드는 필수이고 치는 동안 걸러지며, 겹치면 코드 칸에 알린다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  const code = page.getByRole("textbox", { name: "코드", exact: true })
  const name = page.getByRole("textbox", { name: "이름", exact: true })

  await name.fill("이름만")
  await name.press("Enter")
  await expect(page.getByText("코드를 입력해 주세요.")).toBeVisible()
  expect(sent.create).toBeUndefined()

  // 치는 동안 거른다: 소문자는 대문자로, -는 _로 (FR-009)
  await code.pressSequentially("open-beta")
  await expect(code).toHaveValue("OPEN_BETA")
  await name.fill("오픈 베타")
  await name.press("Enter")
  await expect
    .poll(() => sent.create)
    .toEqual({
      url: expect.stringContaining("/code-groups/GAME_GENRE/codes"),
      body: { code: "OPEN_BETA", name: "오픈 베타" },
    })
  await expect(code).toHaveValue("")

  await code.fill("FPS")
  await name.fill("겹침")
  await page.getByRole("button", { name: "추가", exact: true }).click()
  await expect(page.getByText("이미 있는 코드예요.")).toBeVisible()
  await expect(code).toHaveValue("FPS")
})

test("US1-AC3 이름을 눌러 표 안에서 고치고 엔터로 저장한다. Esc는 되돌리고, 사용 스위치는 바로 저장한다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")

  await page.getByRole("button", { name: "RPG 이름 고치기" }).click()
  const input = page.getByRole("textbox", { name: "RPG 이름" })
  await input.fill("버릴 이름")
  await input.press("Escape")
  await expect(page.getByRole("button", { name: "RPG 이름 고치기" })).toBeVisible()
  expect(sent.modify).toBeUndefined()

  await page.getByRole("button", { name: "RPG 이름 고치기" }).click()
  await page.getByRole("textbox", { name: "RPG 이름" }).fill("롤플레잉")
  await page.getByRole("textbox", { name: "RPG 이름" }).press("Enter")
  await expect
    .poll(() => sent.modify)
    .toEqual({ url: expect.stringContaining("/GAME_GENRE/codes/RPG"), body: { name: "롤플레잉", enabled: true } })

  await page.getByRole("switch", { name: "퍼즐 사용" }).click()
  await expect
    .poll(() => sent.modify)
    .toEqual({ url: expect.stringContaining("/GAME_GENRE/codes/PUZZLE"), body: { name: "퍼즐", enabled: true } })
  // 코드는 고치는 칸이 없다
  await expect(page.getByRole("textbox", { name: "FPS 코드" })).toHaveCount(0)
})

test("US1-AC4 항목 둘을 체크해 한꺼번에 사용 안 함으로 보낸다. 다른 그룹을 고르면 체크가 풀린다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  const toolbar = page.getByRole("toolbar", { name: "고른 항목 처리" })
  await page.getByRole("checkbox", { name: "FPS 고르기" }).click()
  await page.getByRole("checkbox", { name: "RPG 고르기" }).click()
  await expect(toolbar.getByText("2개 선택")).toBeVisible()
  await toolbar.getByRole("button", { name: "사용 안 함" }).click()
  await expect
    .poll(() => sent.enabled)
    .toEqual({
      url: expect.stringContaining("/code-groups/GAME_GENRE/codes/enabled"),
      body: { codes: ["FPS", "RPG"], enabled: false },
    })

  await groupButton(page, "신고 사유").click()
  await expect(page.getByRole("heading", { name: "신고 사유" })).toBeVisible()
  await expect(toolbar.getByText("체크해서 한꺼번에 처리해요.")).toBeVisible()
  await expect(page.getByRole("checkbox", { name: "욕설 고르기" })).not.toBeChecked()
})

test("US1-AC5 키보드로 항목을 옮기면 그 그룹의 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await keyboardMoveUp(page, page.getByRole("button", { name: "RPG 옮기기" }), "RPG", "FPS")
  await expect
    .poll(() => sent.order)
    .toEqual([
      { code: "RPG", sortOrder: 0 },
      { code: "FPS", sortOrder: 1 },
      { code: "PUZZLE", sortOrder: 2 },
    ])
})

test("US1-AC5 키보드로 그룹을 옮기면 그룹 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await keyboardMoveUp(page, page.getByRole("button", { name: "신고 사유 옮기기" }), "신고 사유", "게임 장르")
  await expect
    .poll(() => sent.orderGroups)
    .toEqual([
      { groupCode: "REPORT_REASON", sortOrder: 0 },
      { groupCode: "GAME_GENRE", sortOrder: 1 },
      { groupCode: "EMPTY_GROUP", sortOrder: 2 },
    ])
})

test("US1-AC6 그룹이 없으면 그룹 추가로 첫 그룹을 만들라고 안내한다", async ({ page }) => {
  await mockCodes(page, {}, [])
  await page.goto("/admin/codes")
  await expect(page.getByText("그룹이 없어요. 그룹 추가로 첫 그룹을 만들어요.")).toBeVisible()
  await expect(page.getByRole("button", { name: "그룹 추가" })).toBeVisible()
})

test("US1-AC7 그룹 추가 드로어에서 그룹 코드와 이름을 보낸다. 그룹 코드는 치는 동안 걸러지고 겹치면 알린다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "그룹 추가" }).click()
  const drawer = page.getByRole("dialog", { name: "그룹 추가" })
  await drawer.getByLabel("그룹 코드").pressSequentially("1_event-type 9")
  await expect(drawer.getByLabel("그룹 코드")).toHaveValue("EVENT_TYPE_9")
  await drawer.getByLabel("이름").fill("이벤트 분류")
  await expectNoSeriousA11yViolations(page)

  await drawer.getByLabel("그룹 코드").fill("GAME_GENRE")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect(drawer.getByText("이미 있는 그룹 코드예요.")).toBeVisible()

  await drawer.getByLabel("그룹 코드").fill("EVENT_TYPE")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.createGroup).toEqual({ groupCode: "EVENT_TYPE", name: "이벤트 분류" })
  await expect(drawer).toBeHidden()
})

test("US1-AC8 그룹 설정 드로어에서 이름·설명·사용을 고치고 그룹 코드는 보기만 하며 삭제는 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "그룹 설정" }).click()
  const drawer = page.getByRole("dialog", { name: "그룹 수정" })
  await expect(drawer.getByLabel("그룹 코드")).toHaveValue("GAME_GENRE")
  await expect(drawer.getByLabel("그룹 코드")).toHaveAttribute("readonly", "")
  await expect(drawer.getByRole("button", { name: /삭제/ })).toHaveCount(0)
  await drawer.getByLabel("이름").fill("게임 장르들")
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent.modifyGroup)
    .toEqual({ name: "게임 장르들", description: "게임 등록 화면의 장르", enabled: true })
})

test("US1-AC9 그룹 설정의 사용을 끄면 사용 안 함으로 보낸다. 꺼진 그룹은 목록에서 사용 안 함으로 보인다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await expect(groupButton(page, "신고 사유")).toContainText("사용 안 함")
  await page.getByRole("button", { name: "그룹 설정" }).click()
  const drawer = page.getByRole("dialog", { name: "그룹 수정" })
  await drawer.getByRole("switch", { name: "사용" }).click()
  await expect(drawer.getByText("끄면 이 그룹을 쓰는 선택 상자가 비어요.", { exact: false })).toBeVisible()
  await drawer.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => (sent.modifyGroup as { enabled?: boolean } | undefined)?.enabled).toBe(false)
})

test("US1-AC10 그룹 드로어에서 고치던 내용을 저장하지 않고 닫으면 묻는다 @wide", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "그룹 설정" }).click()
  const drawer = page.getByRole("dialog", { name: "그룹 수정" })
  await drawer.getByLabel("이름").fill("게임 장르 바꿈")
  await drawer.getByRole("button", { name: "취소" }).click()
  const ask = page.getByRole("alertdialog", { name: "저장하지 않고 나갈까요?" })
  await expect(ask).toBeVisible()
  await ask.getByRole("button", { name: "머무르기" }).click()
  await expect(drawer.getByLabel("이름")).toHaveValue("게임 장르 바꿈")
})

test("좁은 폭에서는 그룹 목록 아래에 그룹 상세가 쌓인다", async ({ page }, testInfo) => {
  test.skip((testInfo.project.use.viewport?.width ?? 0) >= 1024, "좁은 폭에서만 본다")
  await mockCodes(page)
  await page.goto("/admin/codes")
  const list = await page.getByRole("list", { name: "코드 그룹 목록" }).boundingBox()
  const table = await page.getByRole("table").boundingBox()
  expect(table?.y ?? 0).toBeGreaterThan((list?.y ?? 0) + (list?.height ?? 0))
})
