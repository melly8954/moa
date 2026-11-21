import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { keyboardMoveUp } from "./support/drag"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 공통코드 (code 헬퍼). 백엔드 대신 API를 가로챈다
// moa: 넓은 폭은 왼쪽 트리 · 오른쪽 상세 패널, 좁은 폭은 트리 + 드로어다. 그룹과 항목은 지우지 않고 사용 여부로 끈다
// (specs/004-admin-codes FR-004~FR-009, US1, research R13). 패널을 쓰는 테스트는 @wide(1280)에서만 돈다

const TREE = [
  {
    groupCode: "GAME_GENRE",
    name: "게임 장르",
    description: "게임 등록 화면의 장르",
    enabled: true,
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
    enabled: false,
    codes: [{ code: "001", name: "욕설", sortOrder: 0, enabled: true }],
  },
  { groupCode: "EMPTY_GROUP", name: "빈 그룹", description: null, enabled: true, codes: [] },
]

type Sent = {
  create?: unknown
  modify?: unknown
  order?: unknown
  enabled?: unknown
  orderGroups?: unknown
  createGroup?: unknown
  modifyGroup?: unknown
}

async function mockCodes(page: Page, sent: Sent = {}, tree: unknown[] = TREE) {
  await signedIn(page, { permissions: ["CODE_MANAGE"] })
  await page.route("**/api/v1/admin/code-groups/tree", (route) => route.fulfill({ json: tree }))
  await page.route("**/api/v1/admin/code-groups", (route) => {
    sent.createGroup = route.request().postDataJSON()
    const body = sent.createGroup as { groupCode: string; name: string }
    return body.groupCode === "GAME_GENRE"
      ? route.fulfill({ status: 409, json: { code: "R002", message: "", timestamp: "", path: "" } })
      : route.fulfill({ status: 201, json: { ...body, enabled: true } })
  })
  for (const groupCode of ["GAME_GENRE", "REPORT_REASON"])
    await page.route(`**/api/v1/admin/code-groups/${groupCode}`, (route) => {
      sent.modifyGroup = route.request().postDataJSON()
      return route.fulfill({ json: { groupCode, ...(sent.modifyGroup as object) } })
    })
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
  // 그룹 순서, 그룹·항목 섞은 일괄 사용 여부 (2026-11-21)
  await page.route("**/api/v1/admin/code-groups/order", (route) => {
    sent.orderGroups = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/code-groups/enabled", (route) => {
    sent.enabled = route.request().postDataJSON()
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

test("US1-AC1 왼쪽 트리와 오른쪽 안내가 보이고, 일괄 처리 줄은 고르기 전에도 같은 자리에 있다 (FR-007 머리 안내) @wide", async ({
  page,
}) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByRole("heading", { name: "코드 관리" })).toBeVisible()
  await expect(page.getByText("지우지 않고 사용 안 함으로 꺼요.")).toBeVisible()
  await expect(page.getByText("왼쪽에서 그룹이나 항목을 고르면 여기서 고쳐요.", { exact: false })).toBeVisible()
  await expect(page.getByText("GAME_GENRE", { exact: true }).filter({ visible: true })).toBeVisible()
  await expect(page.getByText("게임 등록 화면의 장르")).toBeVisible()
  await expect(page.getByRole("row", { name: /퍼즐/ })).toContainText("사용 안 함")
  await expect(page.getByRole("row").filter({ hasText: "REPORT_REASON" })).toContainText("사용 안 함")
  // 삭제는 어디에도 없다 (FR-008)
  await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0)
  // 그룹 행도 체크 칸과 손잡이가 있다. 트리 안에 추가 줄은 없고 트리 위 "추가" 하나다
  await expect(page.getByRole("checkbox", { name: "빈 그룹 고르기" })).toBeVisible()
  await expect(page.getByRole("button", { name: "빈 그룹 옮기기" })).toBeVisible()
  await expect(page.getByRole("table").getByRole("button", { name: /추가/ })).toHaveCount(0)
  await expect(page.getByRole("button", { name: "추가", exact: true })).toBeVisible()

  const toolbar = page.getByRole("toolbar", { name: "고른 그룹·항목 처리" })
  await expect(toolbar.getByText("체크해서 한꺼번에 처리해요.")).toBeVisible()
  await expect(toolbar.getByRole("button", { name: "사용 안 함" })).toBeDisabled()
  const tableTop = async () => (await page.getByRole("table").boundingBox())?.y
  const before = await tableTop()
  await page.getByRole("checkbox", { name: "FPS 고르기" }).click()
  await expect(toolbar.getByText("1개 선택")).toBeVisible()
  expect(await tableTop()).toBe(before)

  // 그룹 행은 접고 편다
  await page.getByRole("button", { name: "게임 장르 접기" }).click()
  await expect(page.getByRole("button", { name: "FPS", exact: true })).toHaveCount(0)
  await page.getByRole("button", { name: "게임 장르 펼치기" }).click()
  await expect(page.getByRole("button", { name: "FPS", exact: true })).toBeVisible()
})

test("US1-AC2 추가에서 소속 그룹을 고르고 이름만 적으면 코드를 비워 보낸다. 겹치는 코드는 코드 칸에 알린다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  // 고른 것이 없으면 소속 그룹은 없음(그룹 추가)이다. 그룹을 고르면 항목 추가로 바뀐다
  await page.getByRole("button", { name: "추가", exact: true }).click()
  await page.getByRole("region", { name: "그룹 추가" }).getByLabel("소속 그룹").selectOption("GAME_GENRE")
  const panel = page.getByRole("region", { name: "항목 추가" })
  await expect(panel.getByText("그룹: 게임 장르")).toBeVisible()
  await panel.getByLabel("이름").fill("퍼즐 2")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.create).toEqual({ name: "퍼즐 2" })
  await expect(panel).toBeHidden()

  // 항목을 고른 채 추가하면 소속 그룹의 처음 값은 그 항목의 그룹이다
  await page.getByRole("button", { name: "RPG", exact: true }).click()
  await page.getByRole("button", { name: "추가", exact: true }).click()
  await expect(panel.getByLabel("소속 그룹")).toHaveValue("GAME_GENRE")
  await panel.getByLabel("이름").fill("겹침")
  // 치는 동안 거른다: 소문자는 대문자로, -는 _로 (FR-009)
  await panel.getByLabel("코드").pressSequentially("ab-c")
  await expect(panel.getByLabel("코드")).toHaveValue("AB_C")
  await panel.getByLabel("코드").fill("001")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect(panel.getByText("이미 있는 코드예요.")).toBeVisible()
})

test("US1-AC3 항목 행을 누르면 패널에서 이름과 사용을 고치고 코드는 보기만 한다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  // 이름 단추가 아닌 행의 코드 칸을 누른다 (2026-11-21)
  await page
    .getByRole("row", { name: /^FPS 고르기/ })
    .getByText("001", { exact: true })
    .filter({ visible: true })
    .click()
  const panel = page.getByRole("region", { name: "항목 수정" })
  await expect(panel.getByLabel("코드")).toHaveValue("001")
  await expect(panel.getByLabel("코드")).toHaveAttribute("readonly", "")
  await panel.getByRole("switch", { name: "사용" }).click()
  await expectNoSeriousA11yViolations(page)
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.modify).toEqual({ name: "FPS", enabled: false })
})

test("US1-AC4 그룹 체크는 그 그룹 자체를 고르고, 다른 그룹의 항목과 섞어 한꺼번에 사용 안 함으로 보낸다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  const toolbar = page.getByRole("toolbar", { name: "고른 그룹·항목 처리" })
  // 그룹을 체크해도 그 그룹의 항목은 골라지지 않는다
  await page.getByRole("checkbox", { name: "신고 사유 고르기" }).click()
  await expect(toolbar.getByText("1개 선택")).toBeVisible()
  await expect(page.getByRole("checkbox", { name: "욕설 고르기" })).not.toBeChecked()

  await page.getByRole("checkbox", { name: "FPS 고르기" }).click()
  await expect(toolbar.getByText("2개 선택")).toBeVisible()
  await toolbar.getByRole("button", { name: "사용 안 함" }).click()
  await expect
    .poll(() => sent.enabled)
    .toEqual({ groupCodes: ["REPORT_REASON"], codes: [{ groupCode: "GAME_GENRE", code: "001" }], enabled: false })
})

test("US1-AC5 키보드로 항목을 옮기면 그 그룹의 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await keyboardMoveUp(page, page.getByRole("button", { name: "RPG 옮기기" }), "RPG", "FPS")
  await expect
    .poll(() => sent.order)
    .toEqual([
      { code: "002", sortOrder: 0 },
      { code: "001", sortOrder: 1 },
      { code: "003", sortOrder: 2 },
    ])
})

test("US1-AC5 키보드로 그룹을 옮기면 그룹 순서를 한 번에 보낸다 @wide", async ({ page }) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  // 접어 두어 그룹 행끼리 맞닿게 한다 (펼쳐 있으면 위의 항목 자리로 가도 그 그룹 자리로 본다)
  await page.getByRole("button", { name: "게임 장르 접기" }).click()
  await keyboardMoveUp(page, page.getByRole("button", { name: "신고 사유 옮기기" }), "신고 사유", "게임 장르")
  await expect
    .poll(() => sent.orderGroups)
    .toEqual([
      { groupCode: "REPORT_REASON", sortOrder: 0 },
      { groupCode: "GAME_GENRE", sortOrder: 1 },
      { groupCode: "EMPTY_GROUP", sortOrder: 2 },
    ])
})

test("US1-AC6 그룹이 없으면 추가로 첫 그룹을 만들라고 안내한다", async ({ page }) => {
  await mockCodes(page, {}, [])
  await page.goto("/admin/codes")
  await expect(page.getByText("그룹이 없어요. 추가로 첫 그룹을 만들어요.")).toBeVisible()
  await expect(page.getByRole("button", { name: "추가", exact: true })).toBeVisible()
})

test("US1-AC7 트리 위 추가(소속 그룹 없음)로 그룹 코드와 이름을 보낸다. 그룹 코드는 치는 동안 걸러지고 겹치면 알린다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "추가", exact: true }).click()
  const panel = page.getByRole("region", { name: "그룹 추가" })
  await expect(panel.getByLabel("소속 그룹")).toHaveValue("")
  await panel.getByLabel("그룹 코드").pressSequentially("1_event-type 9")
  await expect(panel.getByLabel("그룹 코드")).toHaveValue("EVENT_TYPE_9")
  await panel.getByLabel("이름").fill("이벤트 분류")
  await expectNoSeriousA11yViolations(page)

  await panel.getByLabel("그룹 코드").fill("GAME_GENRE")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect(panel.getByText("이미 있는 그룹 코드예요.")).toBeVisible()

  await panel.getByLabel("그룹 코드").fill("EVENT_TYPE")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => sent.createGroup).toEqual({ groupCode: "EVENT_TYPE", name: "이벤트 분류" })
  await expect(panel).toBeHidden()
})

test("US1-AC8 그룹 행(체크박스·손잡이·접기 단추 밖)을 누르면 패널에서 이름·설명·사용을 고치고 그룹 코드는 보기만 하며 삭제는 없다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  // 접기 단추는 접기만 하고 패널을 열지 않는다
  await page.getByRole("button", { name: "게임 장르 접기" }).click()
  await expect(page.getByRole("region", { name: "그룹 수정" })).toHaveCount(0)
  await page.getByRole("button", { name: "게임 장르 펼치기" }).click()
  // 이름 단추가 아닌 그룹 행의 그룹 코드 칸을 누른다 (2026-11-21)
  await page.getByText("GAME_GENRE", { exact: true }).filter({ visible: true }).click()
  const panel = page.getByRole("region", { name: "그룹 수정" })
  await expect(panel.getByLabel("그룹 코드")).toHaveValue("GAME_GENRE")
  await expect(panel.getByLabel("그룹 코드")).toHaveAttribute("readonly", "")
  await expect(panel.getByRole("button", { name: /삭제/ })).toHaveCount(0)
  await panel.getByLabel("이름").fill("게임 장르들")
  await panel.getByRole("button", { name: "저장" }).click()
  await expect
    .poll(() => sent.modifyGroup)
    .toEqual({ name: "게임 장르들", description: "게임 등록 화면의 장르", enabled: true })
})

test("US1-AC9 그룹 패널의 사용을 끄면 사용 안 함으로 보낸다. 꺼진 그룹은 트리에서 사용 안 함으로 보인다 @wide", async ({
  page,
}) => {
  const sent: Sent = {}
  await mockCodes(page, sent)
  await page.goto("/admin/codes")
  await expect(page.getByRole("row").filter({ hasText: "REPORT_REASON" })).toContainText("사용 안 함")
  await page.getByRole("button", { name: "게임 장르 그룹 수정" }).click()
  const panel = page.getByRole("region", { name: "그룹 수정" })
  await panel.getByRole("switch", { name: "사용" }).click()
  await expect(panel.getByText("끄면 이 그룹을 쓰는 선택 상자가 비어요.", { exact: false })).toBeVisible()
  await panel.getByRole("button", { name: "저장" }).click()
  await expect.poll(() => (sent.modifyGroup as { enabled?: boolean } | undefined)?.enabled).toBe(false)
})

test("US1-AC10 고치던 내용을 저장하지 않고 다른 행을 누르면 묻는다 @wide", async ({ page }) => {
  await mockCodes(page)
  await page.goto("/admin/codes")
  await page.getByRole("button", { name: "FPS", exact: true }).click()
  await page.getByRole("region", { name: "항목 수정" }).getByLabel("이름").fill("FPS 바꿈")
  await page.getByRole("button", { name: "RPG", exact: true }).click()
  const ask = page.getByRole("alertdialog", { name: "저장하지 않고 나갈까요?" })
  await expect(ask).toBeVisible()
  await ask.getByRole("button", { name: "머무르기" }).click()
  await expect(page.getByRole("region", { name: "항목 수정" }).getByLabel("이름")).toHaveValue("FPS 바꿈")
})

test("좁은 폭에서는 트리만 보이고 항목은 드로어에서 고친다", async ({ page }, testInfo) => {
  test.skip((testInfo.project.use.viewport?.width ?? 0) >= 1024, "좁은 폭에서만 본다")
  await mockCodes(page)
  await page.goto("/admin/codes")
  await expect(page.getByText("왼쪽에서 그룹이나 항목을 고르면", { exact: false })).toBeHidden()
  await page.getByRole("button", { name: "FPS", exact: true }).click()
  await expect(page.getByRole("dialog").getByLabel("코드")).toHaveValue("001")
})
