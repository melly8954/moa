import { expect, type Locator, type Page } from "@playwright/test"

// 키보드로 끌기 손잡이를 집어 위로 옮기고 target 자리에 놓는다 (dnd-kit 정렬 트리: 메뉴·코드 관리)
//   키를 너무 빨리 누르면 끌기 센서가 단계를 놓친다. 화면 낭독기 안내(live region)로 단계를 기다린다
//   집은 직후의 첫 방향키가 무시될 때가 있다 (CI에서 가끔. 안내가 "{name} 자리로"에 머문다)
//   그래서 아직 제자리일 때만 방향키를 다시 누른다. 한 번이라도 움직였으면 더 누르지 않아 target을 지나치지 않는다
export async function keyboardMoveUp(page: Page, handle: Locator, name: string, target: string) {
  const live = page.locator('[id^="DndLiveRegion"]')
  await handle.focus()
  await page.keyboard.press("Space")
  await expect(live).toContainText(`${name} 자리로`)
  await expect(async () => {
    if ((await live.textContent())?.includes(`${name} 자리로`)) await page.keyboard.press("ArrowUp")
    await expect(live).toContainText(`${target} 자리로`, { timeout: 1_000 })
  }).toPass({ timeout: 10_000 })
  await page.keyboard.press("Space")
}
