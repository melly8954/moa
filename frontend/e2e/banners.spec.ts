import { expect, type Page, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 배너 관리 (specs/016-banner-popup US1) 핵심 흐름. 백엔드 대신 API를 가로챈다
// 상태·순서·권한·이미지 검사는 백엔드 BannerPopupAcceptanceTest가 본다. 여기서는 화면에서만 보이는 동작을 본다
//   US1-AC14 등록 기본값, US1-AC10 미리보기, US1-AC7 칸 아래 이유, US1-AC11·AC12 처리 뒤 고른 것이 풀림 (research R11)

const PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=",
  "base64",
)
const WIDE_URL = "/api/v1/rich-text/images/00000000-0000-4000-8000-000000000001"

const banner = (id: number, name: string, status: string, enabled = true) => ({
  id,
  name,
  wideImageUrl: WIDE_URL,
  startsAt: "2026-12-01T10:00:00+09:00",
  endsAt: null,
  sortOrder: 0,
  enabled,
  status,
})
const BANNERS = [
  banner(1, "윈터컵", "ACTIVE"),
  banner(2, "점검 안내", "DISABLED", false),
  banner(3, "새 시즌", "SCHEDULED"),
]

type Sent = { create?: unknown; enabled?: unknown; bulkDelete?: unknown }

async function mock(page: Page, sent: Sent = {}) {
  await signedIn(page, { me: { role: "ADMIN" }, permissions: ["BANNER_MANAGE"] })
  const list = { content: BANNERS, page: 0, size: 20, totalElements: BANNERS.length, totalPages: 1 }
  await page.route("**/api/v1/admin/banners?*", (route) => route.fulfill({ json: list }))
  await page.route("**/api/v1/admin/banners", (route) => {
    if (route.request().method() !== "POST") return route.fulfill({ json: list })
    sent.create = route.request().postDataJSON()
    return route.fulfill({ status: 201, json: { ...BANNERS[0], id: 9 } })
  })
  await page.route("**/api/v1/admin/banners/enabled", (route) => {
    sent.enabled = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/admin/banners/bulk-delete", (route) => {
    sent.bulkDelete = route.request().postDataJSON()
    return route.fulfill({ status: 204 })
  })
  await page.route("**/api/v1/rich-text/images", (route) =>
    route.fulfill({ status: 201, json: { id: "00000000-0000-4000-8000-000000000001", url: WIDE_URL } }),
  )
  await page.route("**/api/v1/rich-text/images/*", (route) =>
    route.fulfill({ status: 200, contentType: "image/png", body: PNG }),
  )
}

test("US1-AC14·AC10·AC7 배너 등록: 순서 0·켜짐이 기본값이고, 넓은 화면 이미지만 올리면 좁은 틀에도 그 이미지가 보이며, 끝이 시작보다 앞이면 그 칸 아래에 이유가 보인다 (가로 넘침·접근성)", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/banners")
  await page.getByRole("button", { name: "배너 등록" }).click()
  const drawer = page.getByRole("dialog", { name: "배너 등록" })

  // US1-AC14 기본값
  await expect(drawer.getByLabel("순서")).toHaveValue("0")
  await expect(drawer.getByRole("switch", { name: "켜짐" })).toBeChecked()

  // US1-AC10 넓은 화면 이미지만 올렸을 때 미리보기
  await expect(drawer.getByText("이미지를 올리면 여기에 보여요.")).toBeVisible()
  await drawer.getByLabel("넓은 화면 이미지").setInputFiles({ name: "wide.png", mimeType: "image/png", buffer: PNG })
  const preview = drawer.getByRole("region", { name: "미리보기" })
  await expect(preview.getByRole("figure", { name: "넓은 화면" }).locator("img")).toHaveAttribute("src", WIDE_URL)
  await expect(preview.getByRole("figure", { name: "좁은 화면" }).locator("img")).toHaveAttribute("src", WIDE_URL)
  await expect(drawer.getByText("좁은 화면 이미지가 없어 넓은 화면 이미지를 써요.")).toBeVisible()

  await drawer.getByLabel("이름").fill("윈터컵 참가 신청")
  await drawer.getByLabel("대체 문구").fill("moa 윈터컵 5인 팀 대회 참가 신청")
  await drawer.getByLabel("노출 시작 날짜").fill("2026-12-01")
  await drawer.getByLabel("노출 시작 날짜").blur()
  await drawer.getByLabel("노출 시작 시각").fill("10:00")
  await drawer.getByLabel("노출 시작 시각").blur()
  await drawer.getByLabel("노출 끝 날짜").fill("2026-11-30")
  await drawer.getByLabel("노출 끝 날짜").blur()
  await drawer.getByLabel("노출 끝 시각").fill("10:00")
  await drawer.getByLabel("노출 끝 시각").blur()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  // US1-AC7 끝이 시작보다 앞이면 저장되지 않고 그 칸 아래에 이유
  await drawer.getByRole("button", { name: "등록", exact: true }).click()
  await expect(drawer.getByText("노출 끝은 시작보다 뒤여야 해요.")).toBeVisible()
  expect(sent.create).toBeUndefined()

  await drawer.getByLabel("노출 끝 날짜").fill("2026-12-10")
  await drawer.getByLabel("노출 끝 날짜").blur()
  await drawer.getByRole("button", { name: "등록", exact: true }).click()
  await expect(page.getByText("배너를 등록했어요.")).toBeVisible()
  expect(sent.create).toEqual({
    name: "윈터컵 참가 신청",
    wideImageUrl: WIDE_URL,
    altText: "moa 윈터컵 5인 팀 대회 참가 신청",
    startsAt: "2026-12-01T10:00:00+09:00",
    endsAt: "2026-12-10T10:00:00+09:00",
    sortOrder: 0,
    enabled: true,
  })
})

test("US1-AC11·AC12 목록에서 둘을 골라 끄고, 다시 골라 지운다. 처리 뒤 고른 것이 풀리고 체크 칸은 드로어를 열지 않는다 (가로 넘침·접근성)", async ({
  page,
}) => {
  const sent: Sent = {}
  await mock(page, sent)
  await page.goto("/admin/banners")
  const list = page.getByRole("region", { name: "배너 관리" })
  const toolbar = page.getByRole("toolbar", { name: "고른 배너 처리" })
  await expect(toolbar).toContainText("체크해서 한꺼번에 켜고 끄거나 지워요.")
  for (const name of ["켜기", "끄기", "삭제"]) await expect(toolbar.getByRole("button", { name })).toBeDisabled()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  await list.getByRole("checkbox", { name: "윈터컵 선택" }).click()
  await list.getByRole("checkbox", { name: "점검 안내 선택" }).click()
  await expect(page.getByRole("dialog")).toHaveCount(0)
  await expect(toolbar).toContainText("2개 선택")
  await toolbar.getByRole("button", { name: "끄기" }).click()
  await expect(page.getByText("배너 2개를 껐어요.")).toBeVisible()
  expect(sent.enabled).toEqual({ ids: [1, 2], enabled: false })
  await expect(toolbar.getByRole("button", { name: "끄기" })).toBeDisabled()

  await list.getByRole("checkbox", { name: "윈터컵 선택" }).click()
  await list.getByRole("checkbox", { name: "새 시즌 선택" }).click()
  await toolbar.getByRole("button", { name: "삭제" }).click()
  const confirmDialog = page.getByRole("alertdialog", { name: "배너 2개를 지울까요?" })
  await confirmDialog.getByRole("button", { name: "삭제" }).click()
  await expect(page.getByText("배너 2개를 지웠어요.")).toBeVisible()
  expect(sent.bulkDelete).toEqual({ ids: [1, 3] })
  await expect(toolbar.getByRole("button", { name: "삭제" })).toBeDisabled()
})
