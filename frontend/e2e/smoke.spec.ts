// 도구 확인용 테스트. 첫 화면 FR의 AC 테스트가 생기면 지운다
import { expect, test } from "@playwright/test"

test("첫 화면이 열리고 가로 스크롤이 없다", async ({ page }) => {
  await page.goto("/")
  const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth)
  const viewport = page.viewportSize()!
  expect(scrollWidth).toBeLessThanOrEqual(viewport.width)
})
