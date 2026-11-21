import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import SignUpNicknamePage from "@/app/(public)/sign-up/nickname/page"
import { renderWithIntl } from "../intl"

// moa: 소셜 가입 닉네임·약관 화면 (specs/010-social-login US1-AC7, FR-013)
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: vi.fn() }) }))
vi.mock("@/api", () => ({
  api: {
    v1: {
      terms: {
        current: () =>
          Promise.resolve({
            terms: [
              { type: "SERVICE", version: "draft", required: true },
              { type: "PRIVACY", version: "draft", required: true },
            ],
          }),
      },
      oauth: { signUp: vi.fn() },
    },
  },
}))

describe("소셜 가입 닉네임·약관 화면", () => {
  it("US1-AC7 필수 약관 두 개를 모두 체크해야 가입 단추가 켜진다", async () => {
    renderWithIntl(<SignUpNicknamePage />)
    const submit = screen.getByRole("button", { name: "가입하기" })
    await userEvent.type(screen.getByLabelText("닉네임"), "모아킴")

    expect(submit).toBeDisabled()
    await userEvent.click(await screen.findByRole("checkbox", { name: "(필수) 이용약관 동의" }))
    expect(submit).toBeDisabled()
    await userEvent.click(screen.getByRole("checkbox", { name: "(필수) 개인정보 수집·이용 동의" }))
    expect(submit).toBeEnabled()
  })

  it("US1-AC7 약관은 요약이 보이고 펼치면 전문이 보인다", async () => {
    renderWithIntl(<SignUpNicknamePage />)

    const summary = await screen.findByText(/moa 이용약관 요약입니다/)
    expect(summary).toBeInTheDocument()
    expect(screen.queryByText(/제1조 \(목적\)/)).not.toBeInTheDocument()
    await userEvent.click(screen.getAllByRole("button", { name: "전문 보기" })[0])
    expect(await screen.findByText(/제1조 \(목적\)/)).toBeInTheDocument()
  })
})
