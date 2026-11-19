import { expect, test } from "@playwright/test"
import { signedIn } from "./support/api"

// moa: 소셜 로그인 핵심 흐름 (specs/010-social-login US1, tasks T062. 원칙 II "e2e는 핵심 흐름만")
// 백엔드 없이 돈다: 인가 시작(authorize)은 제공자·콜백을 거쳐 닉네임 화면으로 오는 302로 흉내 내고,
// 약관·가입 API는 정해 둔 값을 돌려준다

test("US1-AC1 Google로 계속하기 → 닉네임·약관 → 첫 화면", async ({ page }) => {
  await signedIn(page)
  await page.route("**/api/v1/auth/oauth/providers", (route) => route.fulfill({ json: { providers: ["google"] } }))
  await page.route("**/api/v1/auth/oauth/google/authorize**", (route) =>
    route.fulfill({ status: 302, headers: { location: "/sign-up/nickname" } }),
  )
  await page.route("**/api/v1/terms", (route) =>
    route.fulfill({
      json: {
        terms: [
          { type: "SERVICE", version: "draft", required: true },
          { type: "PRIVACY", version: "draft", required: true },
        ],
      },
    }),
  )
  let signUpBody: unknown
  await page.route("**/api/v1/auth/oauth/sign-up", (route) => {
    signUpBody = route.request().postDataJSON()
    return route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } })
  })

  await page.goto("/sign-in")
  await page.getByRole("button", { name: "Google로 계속하기" }).click()
  await expect(page).toHaveURL(/\/sign-up\/nickname$/)

  const submit = page.getByRole("button", { name: "가입하기" })
  // 약관을 받아 체크박스가 켜질 때까지 기다린다 (화면이 하이드레이션되기 전에 입력하면 값이 지워진다)
  await expect(page.getByRole("checkbox", { name: "(필수) 이용약관 동의" })).toBeEnabled()
  await page.getByLabel("닉네임").fill("모아킴")
  await page.getByRole("checkbox", { name: "(필수) 이용약관 동의" }).click()
  await expect(submit).toBeDisabled()
  await page.getByRole("checkbox", { name: "(필수) 개인정보 수집·이용 동의" }).click()
  await submit.click()

  await expect(page).toHaveURL(/\/$/)
  expect(signUpBody).toEqual({
    nickname: "모아킴",
    agreements: [
      { type: "SERVICE", version: "draft" },
      { type: "PRIVACY", version: "draft" },
    ],
  })
})
