import { expect, type Page, test } from "@playwright/test"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// 서식 있는 글 편집기·보기 (rich-text 헬퍼). 보기 화면은 /dev/rich-text
// 검사용 서버는 SHOW_DEV_PAGES=true로 띄운다. 본문 이미지 API는 가로챈다

const IMAGE_ID = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c"
const IMAGE_URL = `/api/v1/rich-text/images/${IMAGE_ID}`
// 1x1 png
const PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
  "base64",
)

async function open(page: Page, upload?: { status: number; json: unknown }) {
  await page.route("**/api/v1/rich-text/images", (route) =>
    route.fulfill(upload ?? { status: 201, json: { id: IMAGE_ID, url: IMAGE_URL } }),
  )
  await page.route(`**${IMAGE_URL}`, (route) => route.fulfill({ contentType: "image/png", body: PNG }))
  await page.goto("/dev/rich-text")
  const editor = page.getByRole("textbox", { name: "본문" })
  await expect(editor).toBeVisible()
  return editor
}

const view = (page: Page) => page.locator('[data-slot="rich-text"]')
const imageInput = (page: Page) => page.locator('[data-slot="rich-text-image-input"]')

test("편집기·보기: 가로 넘침·접근성 (밝은·어두운 화면)", async ({ page }) => {
  await open(page)
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)
  await page.evaluate(() => document.documentElement.classList.add("dark"))
  await expectNoSeriousA11yViolations(page)
})

test("보기는 허용 목록 밖의 것을 지우고 그린다", async ({ page }) => {
  await open(page)
  await expect(view(page).getByRole("heading", { name: "공지 예시" })).toBeVisible()
  await expect(view(page).locator("script, img, [style], [onclick], [target]")).toHaveCount(0)
  await expect(view(page).getByText("위험한 링크")).not.toHaveAttribute("href")
  await expect(view(page).getByRole("link", { name: "링크", exact: true })).toHaveAttribute(
    "rel",
    "noopener noreferrer",
  )
})

test("도구 막대로 서식을 넣고 저장하면 보기에 그린다", async ({ page }) => {
  const editor = await open(page)
  await editor.click()
  await page.keyboard.type("굵은 글")
  await page.keyboard.press("ControlOrMeta+A")
  await page.getByRole("button", { name: "굵게" }).click()
  await expect(page.getByRole("button", { name: "굵게" })).toHaveAttribute("aria-pressed", "true")
  await page.getByRole("button", { name: "저장" }).click()
  await expect(view(page).locator("strong")).toHaveText("굵은 글")
})

test("비우고 저장하면 칸 오류를 보인다", async ({ page }) => {
  const editor = await open(page)
  await page.getByRole("button", { name: "저장" }).click()
  await expect(editor).toHaveAttribute("aria-invalid", "true")
  await expect(page.getByText("본문을 적어 주세요.")).toBeVisible()
})

test("링크는 http·https·mailto만 넣는다", async ({ page }) => {
  const editor = await open(page)
  await editor.click()
  await page.keyboard.type("누를 곳")
  await page.keyboard.press("ControlOrMeta+A")
  await page.getByRole("button", { name: "링크", exact: true }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByLabel("주소").fill("javascript:alert(1)")
  await dialog.getByRole("button", { name: "적용" }).click()
  await expect(dialog.getByText("http·https 주소나 mailto: 메일 주소만 넣을 수 있어요.")).toBeVisible()
  await dialog.getByLabel("주소").fill("example.com")
  await dialog.getByRole("button", { name: "적용" }).click()
  await expect(dialog).toBeHidden()
  await expect(editor.getByRole("link", { name: "누를 곳" })).toHaveAttribute("href", "https://example.com")
  // 링크 창의 적용이 바깥 폼을 제출하지 않는다 (보기는 처음 예시 그대로)
  await expect(view(page).getByRole("heading", { name: "공지 예시" })).toBeVisible()
})

test("이미지를 올리면 받은 주소를 본문에 넣는다", async ({ page }) => {
  const editor = await open(page)
  await editor.click()
  await imageInput(page).setInputFiles({ name: "a.png", mimeType: "image/png", buffer: PNG })
  await expect(editor.locator("img")).toHaveAttribute("src", IMAGE_URL)
  await page.getByRole("button", { name: "저장" }).click()
  await expect(view(page).locator("img")).toHaveAttribute("src", IMAGE_URL)
})

test("이미지를 올리지 못하면 이유를 알린다", async ({ page }) => {
  await open(page, { status: 400, json: { code: "V001", message: "", timestamp: "", path: "" } })
  await imageInput(page).setInputFiles({ name: "a.png", mimeType: "image/png", buffer: PNG })
  await expect(page.getByText("올릴 수 없는 이미지예요.", { exact: false })).toBeVisible()
  // 크기는 올리기 전에 막는다
  await imageInput(page).setInputFiles({
    name: "big.png",
    mimeType: "image/png",
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1),
  })
  await expect(page.getByText("이미지는 5MB까지 올릴 수 있어요.")).toBeVisible()
})
