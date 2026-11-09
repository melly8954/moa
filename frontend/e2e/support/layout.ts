import AxeBuilder from "@axe-core/playwright"
import { expect, type Page } from "@playwright/test"

// 문서에 가로 넘침이 없어야 한다 (반응형 공통 검사)
export async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() => {
    const el = document.documentElement
    return el.scrollWidth - el.clientWidth
  })
  expect(overflow, "가로 넘침(px)").toBeLessThanOrEqual(0)
}

// 접근성: WCAG 2 A·AA 위반 중 critical·serious가 없어야 한다
// 빼는 규칙·요소는 그 자리에 이유를 적는다 (전체 부품 확인 화면처럼 키트가 아직 고치지 못한 것만)
export async function expectNoSeriousA11yViolations(
  page: Page,
  { disableRules = [], exclude = [] }: { disableRules?: string[]; exclude?: string[] } = {},
) {
  let axe = new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).disableRules(disableRules)
  for (const selector of exclude) axe = axe.exclude(selector)
  const { violations } = await axe.analyze()
  const serious = violations.filter((v) => v.impact === "critical" || v.impact === "serious")
  expect(serious.map((v) => `${v.id}: ${v.help}`)).toEqual([])
}
