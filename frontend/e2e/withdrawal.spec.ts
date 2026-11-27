import { expect, test } from "@playwright/test"
import { signedIn } from "./support/api"
import { expectNoHorizontalOverflow, expectNoSeriousA11yViolations } from "./support/layout"

// moa: 회원 탈퇴·복구 핵심 흐름 (specs/018-member-withdrawal US1-AC5·US2-AC4, tasks T055. 원칙 II "e2e는 핵심 흐름만")
// 백엔드 없이 돈다: 탈퇴·로그인·복구 API는 정해 둔 값을 돌려준다
const PURGE_AT = "2026-12-26T10:00:00+09:00"
const ERROR = { message: "", timestamp: "", path: "" }

test("US1-AC5 계정 관리 탭에서 닉네임을 입력해 탈퇴하면 로그인 화면으로 가고 복구 기한이 보인다", async ({ page }) => {
  await signedIn(page, { me: { name: "모아킴" } })
  let withdrawnWith: unknown
  await page.route("**/api/v1/users/me/withdrawal", (route) => {
    withdrawnWith = route.request().postDataJSON()
    return route.fulfill({ json: { purgeAt: PURGE_AT } })
  })

  await page.goto("/account/profile/manage")
  await expect(page.getByRole("heading", { name: "마이페이지" })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  await page.getByRole("button", { name: "탈퇴하기" }).click()
  const dialog = page.getByRole("alertdialog")
  await expect(dialog.getByText(/30일 안에 다시 로그인하면/)).toBeVisible()
  await expectNoSeriousA11yViolations(page)
  await expect(dialog.getByRole("button", { name: "탈퇴하기" })).toBeDisabled()
  await dialog.getByLabel("닉네임").fill("모아킴")
  // 탈퇴한 뒤에는 재발급이 막힌다 (새로 연 로그인 화면이 로그인을 잇지 않는다)
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ status: 401, json: { code: "A003", ...ERROR } }),
  )
  await dialog.getByRole("button", { name: "탈퇴하기" }).click()

  await expect(page).toHaveURL(/\/sign-in\?withdrawnUntil=/)
  await expect(page.getByText("탈퇴했어요. 2026-12-26 10:00까지 다시 로그인하면 복구할 수 있어요.")).toBeVisible()
  expect(withdrawnWith).toEqual({ nickname: "모아킴" })
})

test("US2-AC4 탈퇴한 계정에 맞는 비밀번호로 로그인하면 복구 화면이 보이고, 복구하면 로그인되어 첫 화면으로 간다", async ({
  page,
}) => {
  await signedIn(page)
  await page.route("**/api/v1/auth/token/refresh", (route) =>
    route.fulfill({ status: 401, json: { code: "A003", ...ERROR } }),
  )
  await page.route("**/api/v1/auth/login", (route) => route.fulfill({ status: 401, json: { code: "A401", ...ERROR } }))
  let recovered = false
  await page.route("**/api/v1/auth/recovery", (route) => {
    if (route.request().method() === "POST") {
      recovered = true
      return route.fulfill({ json: { accessToken: "e2e-token", tokenType: "Bearer", expiresIn: 900 } })
    }
    return route.fulfill({ json: { purgeAt: PURGE_AT } })
  })

  await page.goto("/sign-in")
  await page.getByLabel("아이디").fill("e2e")
  await page.getByLabel("비밀번호").fill("e2e-pass")
  await page.getByRole("button", { name: "로그인" }).click()

  await expect(page).toHaveURL(/\/sign-in\/recover$/)
  await expect(page.getByText(/2026-12-26 10:00에 계정과 개인정보가 완전히 삭제돼요/)).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectNoSeriousA11yViolations(page)

  await page.getByRole("button", { name: "복구하기" }).click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole("main")).toBeVisible()
  expect(recovered).toBe(true)
})
