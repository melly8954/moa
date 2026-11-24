import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 팝업 관리 (specs/016-banner-popup US2) 핵심 흐름. 백엔드 대신 API를 가로챈다
// 상태·순서·권한·내용 거르기는 백엔드 BannerPopupAcceptanceTest가 본다
//   여기서는 넓은 드로어 안의 편집기로 팝업을 등록하는 것과 US2-AC5 처리 뒤 고른 것이 풀리는 것을 본다 (research R9·R11)

const popup = (id: number, title: string, status: string) => ({
  id,
  title,
  startsAt: "2026-12-01T10:00:00+09:00",
  endsAt: null,
  sortOrder: 0,
  enabled: true,
  status,
})
const POPUPS = [
  popup(1, "시즌 대회 안내", "ACTIVE"),
  popup(2, "점검 안내", "SCHEDULED"),
  popup(3, "새해 인사", "ACTIVE"),
]

type Sent = { create?: { title: string; content: string }; bulkDelete?: unknown }

async function mock(page: Page, sent: Sent = {}) {
  await signedIn(page, { me: { role: "ADMIN" }, permissions: ["POPUP_MANAGE"] })
  const list = { content: POPUPS, page: 0, size: 20, totalElements: POPUPS.length, totalPages: 1 }
  await page.route("**/api/v1/admin/popups?*", (route) => route.fulfill({ json: list }))
  await page.route("**/api/v1/admin/popups", (route) => {
    if (route.request().method() !== "POST") return route.fulfill({ json: list })
    sent.create = route.request().postDataJSON()
    return route.fulfill({ status: 201, json: { ...POPUPS[0], id: 9, content: "<p>x</p>" } })
  })
  await page.route("**/api/v1/admin/popups/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
}

test("US2-AC1 팝업 등록: 넓은 드로어 안의 편집기로 내용을 써서 등록한다. 드로어는 넘치지 않는다 (가로 넘침·접근성)", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/popups")
  await page.getByRole("button", { name: "팝업 등록" }).click()
  const drawer = page.getByRole("dialog", { name: "팝업 등록" })

  await drawer.getByLabel("제목").fill("12월 시즌 대회 열려요")
  const editor = drawer.locator('[contenteditable="true"]')
  await editor.click()
  await editor.pressSequentially("참가 신청은 12월 10일까지예요.")
  await drawer.getByLabel("노출 시작 날짜").fill("2026-12-01")
  await drawer.getByLabel("노출 시작 날짜").blur()
  await drawer.getByLabel("노출 시작 시각").fill("10:00")
  await drawer.getByLabel("노출 시작 시각").blur()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  await drawer.getByRole("button", { name: "등록", exact: true }).click()
  await expect(page.getByText("팝업을 등록했어요.")).toBeVisible()
  expect(sent.create?.title).toBe("12월 시즌 대회 열려요")
  expect(sent.create?.content).toContain("참가 신청은 12월 10일까지예요.")
})

test("US2-AC5 목록에서 팝업 둘을 골라 지우면 고른 것이 풀린다", async ({ page }) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/popups")
  const list = page.getByRole("region", { name: "팝업 관리" })
  const toolbar = page.getByRole("toolbar", { name: "고른 팝업 처리" })

  await list.getByRole("checkbox", { name: "시즌 대회 안내 선택" }).click()
  await list.getByRole("checkbox", { name: "새해 인사 선택" }).click()
  await expect(toolbar).toContainText("2개 선택")
  await toolbar.getByRole("button", { name: "삭제" }).click()
  await page.getByRole("alertdialog", { name: "팝업 2개를 지울까요?" }).getByRole("button", { name: "삭제" }).click()
  await expect(page.getByText("팝업 2개를 지웠어요.")).toBeVisible()
  expect(sent.bulkDelete).toEqual({ ids: [1, 3] })
  await expect(toolbar.getByRole("button", { name: "삭제" })).toBeDisabled()
})
