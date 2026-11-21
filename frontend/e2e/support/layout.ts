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
  // 전환(transition)이 끝난 뒤에 본다. 사이드바 접기·어두운 화면 전환 중의 흐린 글자를 대비 위반으로 잡는다
  //   끝없는 애니메이션(불러오는 중 표시 등)은 기다리지 않고, 길어도 2초까지만 기다린다 (moa에서 먼저, 키트로 옮길 것)
  await page.evaluate(() =>
    Promise.race([
      Promise.all(
        document
          .getAnimations()
          .filter((a) => a.effect?.getTiming().iterations !== Infinity)
          .map((a) => a.finished.catch(() => undefined)),
      ),
      new Promise((resolve) => setTimeout(resolve, 2000)),
    ]),
  )
  let axe = new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).disableRules(disableRules)
  for (const selector of exclude) axe = axe.exclude(selector)
  const { violations } = await axe.analyze()
  const serious = violations.filter((v) => v.impact === "critical" || v.impact === "serious")
  // moa: 어느 요소인지 알 수 있게 대상(선택자)도 적는다. 실패 때만 보인다 (키트로 옮길 것)
  expect(serious.map((v) => `${v.id}: ${v.help} @ ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`)).toEqual([])
}
