import { expect, type Page, test } from "@playwright/test"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 목록 화면 공통 모양(/dev/list, 가짜 회원 87명): 검색 영역 → 주소 → 표
// 검사용 서버는 SHOW_DEV_PAGES=true로 띄운다 (playwright.config.ts)

const rows = (page: Page) => page.locator("tbody tr")
const statusCells = (page: Page) => page.locator("tbody tr td:nth-child(5)")

async function open(page: Page, query = "", { loaded = true } = {}) {
  await page.goto(`/dev/list${query}`)
  await expect(page.getByRole("heading", { name: "회원 목록" })).toBeVisible()
  if (loaded) await expect(page.getByText(/^총 \d+건$/)).toBeVisible()
}

test("목록 화면: 가로 넘침·접근성 (밝은·어두운 화면)", async ({ page }) => {
  await open(page)
  await expectNoHorizontalOverflow(page)
  // 좁은 화면에서도 글자 열(이름)이 폭 0으로 줄지 않는다 (표 안에서 가로로 넘긴다)
  await expect(rows(page).first().locator("td").nth(2)).toBeVisible()
  await expectNoSeriousA11yViolations(page)
  await page.evaluate(() => document.documentElement.classList.add("dark"))
  await expectNoSeriousA11yViolations(page)
})

test("조건은 조회를 눌러야 반영되고, 주소에 남고, 1쪽으로 돌아간다", async ({ page }) => {
  await open(page, "?page=3")
  await page.getByRole("button", { name: "중지", exact: true }).click()
  await expect(page).toHaveURL(/\?page=3$/)

  await page.getByRole("button", { name: "조회" }).click()
  await expect(page).toHaveURL(/\?status=STOPPED$/)
  await expect(page.getByText("총 21건")).toBeVisible()
  for (const text of await statusCells(page).allInnerTexts()) expect(text).toContain("중지")

  // 새로고침해도 같은 목록
  await page.reload()
  await expect(page.getByText("총 21건")).toBeVisible()
  await expect(page.getByRole("button", { name: "중지", exact: true })).toHaveAttribute("aria-pressed", "true")
})

test("검색어 칸에서 Enter로 조회하고, 결과가 없으면 '조건에 맞는 결과가 없어요'", async ({ page }) => {
  await open(page)
  await page.getByLabel("이름").fill("없는이름")
  await page.getByLabel("이름").press("Enter")
  await expect(page).toHaveURL(/keyword=/)
  await expect(page.getByText("조건에 맞는 결과가 없어요.")).toBeVisible()
})

test("초기화는 기본값으로 되돌리고 바로 조회한다", async ({ page }) => {
  await open(page, "?status=STOPPED&grade=%EA%B3%A8%EB%93%9C&page=2")
  await page.getByRole("button", { name: "초기화" }).click()
  await expect(page).toHaveURL(/\/dev\/list$/)
  await expect(page.getByText("총 87건")).toBeVisible()
  await expect(page.getByLabel("등급")).toHaveValue("")
})

test("쪽 이동과 쪽 크기 (쪽 크기를 바꾸면 1쪽)", async ({ page }) => {
  await open(page)
  await expect(rows(page)).toHaveCount(20)
  await page.getByRole("button", { name: "2쪽" }).click()
  await expect(page).toHaveURL(/\?page=2$/)
  await expect(page.getByRole("button", { name: "2쪽" })).toHaveAttribute("aria-current", "page")

  await page.getByLabel("한 쪽에 보일 개수").selectOption("50")
  await expect(page).toHaveURL(/\?size=50$/)
  await expect(rows(page)).toHaveCount(50)
})

test("열 이름을 누르면 서버 정렬: 오름차순 → 내림차순 → 기본", async ({ page }) => {
  await open(page)
  const header = page.getByRole("columnheader", { name: /가입일/ })
  await header.getByRole("button").click()
  await expect(page).toHaveURL(/sort=joinedOn%2Casc/)
  await expect(header).toHaveAttribute("aria-sort", "ascending")
  await header.getByRole("button").click()
  await expect(header).toHaveAttribute("aria-sort", "descending")
})

test("기간: 빠른 선택은 두 칸을 채우고, 직접 친 날짜는 칸을 떠날 때 맞춘다", async ({ page }) => {
  await open(page)
  await page.getByRole("button", { name: "기간 고르기" }).click()
  await page.getByRole("button", { name: "최근 7일" }).click()
  await expect(page.getByLabel("가입일 시작일")).toHaveValue(/^\d{4}-\d{2}-\d{2}$/)
  await expect(page.getByLabel("가입일 종료일")).toHaveValue(/^\d{4}-\d{2}-\d{2}$/)

  await page.getByLabel("가입일 시작일").fill("2026.1.5")
  await page.getByLabel("가입일 시작일").blur()
  await expect(page.getByLabel("가입일 시작일")).toHaveValue("2026-01-05")

  await page.getByLabel("가입일 종료일").fill("2026-13-40")
  await page.getByLabel("가입일 종료일").blur()
  await expect(page.getByLabel("가입일 종료일")).toHaveAttribute("aria-invalid", "true")
})

test("서버 오류면 문의 번호와 다시 시도를 보인다", async ({ page }) => {
  await open(page, "?keyword=error", { loaded: false })
  await expect(page.locator("[data-slot=alert]")).toContainText("문의 번호: demo-trace-id")
  await expect(page.getByRole("button", { name: "다시 시도" })).toBeVisible()
})

test("행 선택과 확인 창", async ({ page }) => {
  await open(page)
  await page.getByRole("checkbox", { name: "이 쪽 모두 선택" }).click()
  await page.getByRole("button", { name: "선택 삭제" }).click()
  await expect(page.getByRole("alertdialog", { name: "20명을 지울까요?" })).toBeVisible()
  await page.getByRole("button", { name: "취소" }).click()
  await expect(page.getByRole("alertdialog")).toBeHidden()
})
