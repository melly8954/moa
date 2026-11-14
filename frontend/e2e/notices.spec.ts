import { expect, type Page, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 공지사항 (specs/006-notices) 핵심 흐름. 백엔드 대신 API를 가로챈다
// 권한·거르기·순서는 백엔드 NoticesAcceptanceTest가 확인한다. 여기서는 화면이 API와 맞게 도는지 본다

// 점이 있는 "공지" 메뉴의 접근성 이름 (이름과 화면 낭독기용 글자가 따로 있어 쉼표 앞에 공백이 들어간다)
const NOTICE_MENU_WITH_DOT = /^공지\s*,\s*새 공지$/

const PUBLISHED = {
  id: 1,
  title: "서버 점검 안내",
  content: "<h2>점검 시간</h2><p><strong>새벽 2시</strong>부터 한 시간</p>",
  pinned: true,
  publishedAt: "2026-11-14T09:00:00+09:00",
}
const NORMAL = {
  id: 2,
  number: 7,
  title: "이벤트 당첨자 발표",
  pinned: false,
  publishedAt: "2026-11-13T09:00:00+09:00",
}

async function mockPublic(page: Page) {
  await page.route("**/api/v1/notices?*", (route) =>
    route.fulfill({
      json: {
        content: [
          { id: PUBLISHED.id, number: 8, title: PUBLISHED.title, pinned: true, publishedAt: PUBLISHED.publishedAt },
          NORMAL,
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
      },
    }),
  )
  await page.route("**/api/v1/notices/1", (route) => route.fulfill({ json: PUBLISHED }))
  await page.route("**/api/v1/notices/99", (route) =>
    route.fulfill({ status: 404, json: { code: "R001", message: "", timestamp: "", path: "" } }),
  )
}

test("사용자 공지: 로그인 없이 목록과 상세를 본다 (가로 넘침·접근성)", async ({ page }) => {
  await signedOut(page)
  await mockPublic(page)
  await page.goto("/notices")
  // 표: 번호(게시 순번)·제목·게시일. 고정 공지는 번호 자리에 고정 표시 (FR-010)
  await expect(page.getByRole("columnheader")).toHaveText(["번호", "제목", "게시 날짜"])
  await expect(page.getByRole("row", { name: /서버 점검 안내/ })).toContainText("고정")
  await expect(page.getByRole("row", { name: /서버 점검 안내/ })).not.toContainText("8")
  await expect(page.getByRole("row", { name: /이벤트 당첨자 발표/ })).toContainText("7")
  await expect(page.getByRole("row", { name: /이벤트 당첨자 발표/ })).toContainText("2026-11-13")
  // 사용자 셸 안에 있다: 사이드바(좁은 폭은 서랍 단추)와 로그인 단추 (001 FR-005, FR-010)
  await expect(page.getByRole("button", { name: "Toggle Sidebar" }).first()).toBeVisible()
  await expect(page.getByRole("link", { name: "로그인" }).first()).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  await page.getByRole("link", { name: /서버 점검 안내/ }).click()
  await expect(page.getByRole("heading", { level: 1, name: "서버 점검 안내" })).toBeVisible()
  await expect(page.getByRole("heading", { level: 2, name: "점검 시간" })).toBeVisible()
  await expect(page.getByText("새벽 2시")).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
})

test("사용자 공지: 없는 공지는 안내와 목록 링크를 보인다", async ({ page }) => {
  await signedOut(page)
  await mockPublic(page)
  await page.goto("/notices/99")
  await expect(page.getByRole("heading", { name: "없는 공지예요." })).toBeVisible()
  await expect(page.getByRole("link", { name: "공지 목록으로" })).toBeVisible()
})

test("새 공지: 공지 메뉴 옆에 점이 보이고, 목록을 열면 확인을 기록해 점이 사라진다", async ({ page, viewport }) => {
  test.skip((viewport?.width ?? 0) < 768, "좁은 폭은 사이드바가 서랍이라 메뉴가 닫혀 있다")
  const seen: string[] = []
  await signedIn(page)
  await mockPublic(page)
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: [] }))
  await page.route("**/api/v1/notices/new", (route) => route.fulfill({ json: { hasNew: seen.length === 0 } }))
  await page.route("**/api/v1/notices/seen", (route) => {
    seen.push(route.request().method())
    return route.fulfill({ status: 204 })
  })
  await page.goto("/")
  // 점은 화면 낭독기에 "새 공지"로 읽힌다 (FR-010)
  await expect(page.getByRole("link", { name: NOTICE_MENU_WITH_DOT })).toBeVisible()

  await page.getByRole("link", { name: NOTICE_MENU_WITH_DOT }).click()
  await expect(page.getByRole("row", { name: /서버 점검 안내/ })).toBeVisible()
  await expect.poll(() => seen).toEqual(["PUT"])

  await page.goto("/")
  await expect(page.getByRole("link", { name: "공지", exact: true })).toBeVisible()
})

test("공지 관리: 공지를 써서 게시한다 (가로 넘침·접근성, 밝은·어두운 화면)", async ({ page }) => {
  const created: unknown[] = []
  await signedIn(page, { permissions: ["NOTICE_MANAGE"] })
  await page.route("**/api/v1/menus/me", (route) => route.fulfill({ json: [] }))
  await page.route("**/api/v1/admin/notices", (route) => {
    created.push(route.request().postDataJSON())
    return route.fulfill({
      status: 201,
      json: {
        id: 5,
        title: "새 공지",
        content: "<p>본문</p>",
        status: "PUBLISHED",
        pinned: false,
        publishedAt: "2026-11-14T10:00:00+09:00",
        createdAt: "2026-11-14T10:00:00+09:00",
        updatedAt: "2026-11-14T10:00:00+09:00",
      },
    })
  })
  await page.route("**/api/v1/admin/notices/5", (route) =>
    route.fulfill({
      json: {
        id: 5,
        title: "새 공지",
        content: "<p>본문</p>",
        status: "PUBLISHED",
        pinned: false,
        publishedAt: "2026-11-14T10:00:00+09:00",
        createdAt: "2026-11-14T10:00:00+09:00",
        updatedAt: "2026-11-14T10:00:00+09:00",
      },
    }),
  )
  await page.goto("/admin/notices/new")
  await expect(page.getByRole("heading", { level: 1, name: "공지 쓰기" })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
  // 화면 모드를 바꾸면 색이 전환(transition)되므로 끝난 뒤에 대비를 본다
  await page.evaluate(() => document.documentElement.classList.add("dark"))
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished)))
  await expectNoSeriousA11yViolations(page)
  await page.evaluate(() => document.documentElement.classList.remove("dark"))

  await page.getByRole("button", { name: "게시" }).click()
  await expect(page.getByText("제목을 적어 주세요.")).toBeVisible()
  await expect(page.getByText("본문을 적어 주세요.")).toBeVisible()

  await page.getByRole("textbox", { name: "제목" }).fill("새 공지")
  await page.getByRole("textbox", { name: "본문" }).click()
  await page.keyboard.type("본문")
  await page.getByRole("button", { name: "게시" }).click()

  await expect(page).toHaveURL(/\/admin\/notices\/5$/)
  expect(created).toEqual([{ title: "새 공지", content: "<p>본문</p>", pinned: false, publish: true }])
  await expect(page.getByRole("heading", { level: 1, name: "공지 고치기" })).toBeVisible()
})
