// FR-MEM-001 회원가입 화면 AC (docs/design/conventions.md 7절)
// API 응답은 page.route로 대신하고 화면 동작만 본다. API 동작은 백엔드 SignUpAcceptanceTest가 본다
import { expect, type Page, test } from "@playwright/test"

type PhoneConflict = { accountStatus: string; maskedEmail: string | null }

const SIGN_UP_EMAIL = "newbie@naver.com"
const MASKED_EMAIL = "ab***@naver.com"

function signUpState(phoneVerified: boolean, phoneConflict: PhoneConflict | null) {
  return {
    email: SIGN_UP_EMAIL,
    provider: null,
    phoneVerified,
    phoneConflict,
    expiresAt: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
  }
}

/** 가입 진행 API를 대신한다. 인증 번호 확인은 conflictCode로 거부하고, 그 뒤 상태 조회는 conflict를 준다 */
async function mockPhoneConflict(page: Page, conflictCode: string, conflict: PhoneConflict) {
  let confirmed = false
  await page.route("**/api/v1/sign-ups/current", (route) =>
    route.fulfill({ json: signUpState(false, confirmed ? conflict : null) }),
  )
  await page.route("**/api/v1/sign-ups/phone-verifications", (route) =>
    route.fulfill({
      status: 201,
      json: {
        expiresAt: new Date(Date.now() + 3 * 60 * 1000).toISOString(),
        resendAvailableAt: new Date(Date.now() + 60 * 1000).toISOString(),
      },
    }),
  )
  await page.route("**/api/v1/sign-ups/phone-confirmations", (route) => {
    confirmed = true
    return route.fulfill({
      status: 409,
      json: { code: conflictCode, message: "휴대폰 번호가 기존 계정과 겹칩니다." },
    })
  })
}

async function submitPhoneVerification(page: Page) {
  await page.goto("/signup/phone")
  await page.getByLabel("생년월일").fill("2000-01-31")
  await page.getByLabel("휴대폰 번호").fill("01012345678")
  await page.getByRole("button", { name: /인증 번호.*받기/ }).click()
  await page.getByLabel("인증 번호", { exact: true }).fill("123456")
  await page.getByRole("button", { name: "다음" }).click()
}

test("FR-MEM-001 AC-11: 휴대폰 번호가 활동 중인 기존 계정과 겹치면 가려진 기존 계정 이메일과 연결 방법 안내가 보인다", async ({
  page,
}) => {
  await mockPhoneConflict(page, "B011", { accountStatus: "ACTIVE", maskedEmail: MASKED_EMAIL })

  await submitPhoneVerification(page)

  const alert = page.getByRole("alert").filter({ hasText: MASKED_EMAIL })
  await expect(alert).toBeVisible()
  await expect(alert).toContainText("로그인 수단 관리")
})

test("FR-MEM-001 AC-11: 휴대폰 번호가 정지된 계정과 겹치면 가려진 이메일과 정지가 끝난 뒤 로그인하라는 안내가 보인다", async ({
  page,
}) => {
  await mockPhoneConflict(page, "B012", { accountStatus: "SUSPENDED", maskedEmail: MASKED_EMAIL })

  await submitPhoneVerification(page)

  const alert = page.getByRole("alert").filter({ hasText: MASKED_EMAIL })
  await expect(alert).toBeVisible()
  await expect(alert).toContainText(/정지가 끝난 뒤.*로그인/)
})

test("FR-MEM-001 AC-11: 휴대폰 번호가 탈퇴 유예 중 계정과 겹치면 이메일 없이 복구·재가입 안내가 보인다", async ({
  page,
}) => {
  await mockPhoneConflict(page, "B013", { accountStatus: "WITHDRAWN", maskedEmail: null })

  await submitPhoneVerification(page)

  const alert = page.getByRole("alert").filter({ hasText: "고객 문의" })
  await expect(alert).toBeVisible()
  await expect(alert).toContainText(/다시 가입/)
  await expect(alert).not.toContainText("@")
})

test("FR-MEM-001 AC-12: 가입 화면에 수집 항목(이메일, 생년월일, 휴대폰 번호)과 목적이 적힌 필수 동의가 있다", async ({
  page,
}) => {
  await page.route("**/api/v1/sign-ups/current", (route) => route.fulfill({ json: signUpState(true, null) }))

  await page.goto("/signup/profile")

  await expect(page.getByRole("checkbox", { name: /\[필수\].*이용약관/ })).toBeVisible()
  await expect(page.getByRole("checkbox", { name: /\[필수\].*개인정보 수집/ })).toBeVisible()
  await expect(page.getByText(/이메일\s*:.*로그인/)).toBeVisible()
  await expect(page.getByText(/생년월일\s*:.*만 14세/)).toBeVisible()
  await expect(page.getByText(/휴대폰 번호\s*:.*1인 1계정/)).toBeVisible()
})
