import { expect, test } from "@playwright/test"
import { signedIn, signedOut } from "./support/api"

// 인증 흐름: 로그인 뒤 돌아가기(?next=), 임시 비밀번호 변경, 저장하지 않고 나가기

test("로그인이 필요한 화면을 열면 ?next=를 붙여 로그인 화면으로 보내고, 로그인하면 그 화면으로 돌아간다", async ({
  page,
}) => {
  await signedOut(page)
  await page.goto("/account/password?from=e2e")
  await expect(page).toHaveURL(/\/sign-in\?next=%2Faccount%2Fpassword%3Ffrom%3De2e$/)

  await page.unrouteAll()
  await signedIn(page)
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  await page.getByLabel("아이디").fill("e2e")
  await page.getByLabel("비밀번호").fill("e2e-pass")
  await page.getByRole("button", { name: "로그인" }).click()
  await expect(page).toHaveURL(/\/account\/password\?from=e2e$/)
})

test("다른 사이트 주소는 ?next=로 받지 않는다", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } }),
  )
  await page.goto("/sign-in?next=//evil.example")
  await page.getByLabel("아이디").fill("e2e")
  await page.getByLabel("비밀번호").fill("e2e-pass")
  await page.getByRole("button", { name: "로그인" }).click()
  await expect(page).toHaveURL(/\/$/)
})

test("임시 비밀번호 사용자는 비밀번호 변경 화면에만 있고, 바꾸면 돌아간다", async ({ page }) => {
  let required = true
  await signedIn(page)
  await page.route("**/api/v1/users/me", (route) =>
    route.fulfill({
      json: { id: 2, loginId: "e2e", name: "e2e", role: "USER", passwordChangeRequired: required },
    }),
  )
  await page.route("**/api/v1/users/me/password", (route) => {
    required = false
    return route.fulfill({ status: 204 })
  })

  await page.goto("/")
  await expect(page).toHaveURL(/\/account\/password$/)
  await expect(page.getByText("임시 비밀번호로 로그인했어요")).toBeVisible()
  await expect(page.getByRole("button", { name: "취소" })).toHaveCount(0)

  await page.getByLabel("지금 비밀번호").fill("temp-pass-1")
  await page.getByLabel("새 비밀번호", { exact: true }).fill("new-pass-123")
  await page.getByLabel("새 비밀번호 확인").fill("new-pass-123")
  await page.getByRole("button", { name: "바꾸기" }).click()
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByText("비밀번호를 바꿨어요.")).toBeVisible()
})

test("지금 비밀번호가 틀리면(칸 오류 없는 V001) 그 칸에 알린다", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/users/me/password", (route) =>
    route.fulfill({ status: 400, json: { code: "V001", message: "", timestamp: "", path: "" } }),
  )
  await page.goto("/account/password")
  await page.getByLabel("지금 비밀번호").fill("wrong-pass")
  await page.getByLabel("새 비밀번호", { exact: true }).fill("new-pass-123")
  await page.getByLabel("새 비밀번호 확인").fill("new-pass-123")
  await page.getByRole("button", { name: "바꾸기" }).click()
  await expect(page.getByText("지금 비밀번호가 맞지 않아요.")).toBeVisible()
})

test("입력한 채 취소를 누르면 되묻고, 머무르기면 그대로 있다", async ({ page }) => {
  await signedIn(page)
  await page.goto("/account/password")
  await page.getByLabel("지금 비밀번호").fill("typed")

  const cancel = page.getByRole("main").getByRole("button", { name: "취소" })
  await cancel.click()
  const dialog = page.getByRole("alertdialog", { name: "저장하지 않고 나갈까요?" })
  await expect(dialog).toBeVisible()
  await dialog.getByRole("button", { name: "머무르기" }).click()
  await expect(page).toHaveURL(/\/account\/password$/)
  await expect(page.getByLabel("지금 비밀번호")).toHaveValue("typed")
  await expect(dialog).toBeHidden()

  await cancel.click()
  await page.getByRole("alertdialog").getByRole("button", { name: "나가기" }).click()
  await expect(page).toHaveURL(/\/$/)
})

test("입력한 채 메뉴 링크를 누르면 되묻는다 @wide", async ({ page }) => {
  await signedIn(page)
  await page.goto("/account/password")
  await page.getByLabel("지금 비밀번호").fill("typed")
  // 셸마다 모양이 달라도(두 겹 사이드바형은 아이콘만) 메뉴는 이름이 붙은 링크다
  await page.getByRole("link", { name: "홈", exact: true }).first().click()
  await expect(page.getByRole("alertdialog", { name: "저장하지 않고 나갈까요?" })).toBeVisible()
  await page.getByRole("alertdialog").getByRole("button", { name: "나가기" }).click()
  await expect(page).toHaveURL(/\/$/)
})
