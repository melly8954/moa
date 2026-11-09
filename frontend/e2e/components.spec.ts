import { expect, test } from "@playwright/test"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 전체 부품 확인 화면(/dev/components): 밝은·어두운 화면 모두 넘침·접근성 검사
// 검사용 서버는 SHOW_DEV_PAGES=true로 띄운다 (playwright.config.ts)
//
// 빼 둔 것 (README "알려진 제약")
// - color-contrast: 테마 기본 색이 기준(4.5)에 못 미친다. 빨간 버튼·배지 4.0, 오류 안내 설명 4.49, Kbd 4.34.
//   디자인을 정할 때 토큰으로 고치고 이 줄을 지운다
// - resizable-handle: react-resizable-panels가 aria-valuenow를 주지 않는다
// - scroll-area 안쪽: Base UI가 스크롤 영역에 키보드 초점을 주지 않는다. 실제 화면은 안에 초점 갈 곳(링크·버튼)을 둔다
const KNOWN = {
  disableRules: ["color-contrast"],
  exclude: ["[data-slot=resizable-handle]", "[data-slot=scroll-area-viewport]"],
}

for (const dark of [false, true]) {
  test(`전체 부품 ${dark ? "어두운" : "밝은"} 화면: 가로 넘침·접근성`, async ({ page }) => {
    await page.goto("/dev/components")
    await expect(page.getByRole("heading", { name: "전체 부품 확인" })).toBeVisible()
    if (dark) await page.getByRole("switch", { name: "어두운 화면" }).click()
    await expectNoHorizontalOverflow(page)
    await expectNoSeriousA11yViolations(page, KNOWN)
  })
}
