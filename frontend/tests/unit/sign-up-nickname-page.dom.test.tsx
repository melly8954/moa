import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import SignUpNicknamePage from "@/app/(public)/sign-up/nickname/page"
import { renderWithIntl } from "../intl"

// moa: 소셜 가입 닉네임·약관 화면 (specs/010-social-login US1-AC7, FR-013)
// 이용약관 전문은 공개 문서 API에서 받는다 (specs/012-terms-privacy FR-005·FR-010)
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
      legal: {
        version: () =>
          Promise.resolve({
            document: "terms",
            version: "draft",
            effectiveDate: "2026-11-01",
            title: "moa 이용약관",
            sections: [{ id: "purpose", heading: "제1조 (목적)", html: "<p>moa 이용약관 전문입니다.</p>" }],
            state: "CURRENT",
            currentVersion: "draft",
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

  it("US1-AC7 약관은 전문이 상자에 바로 보이고, 아래 링크는 문서 화면을 새 탭에서 연다", async () => {
    renderWithIntl(<SignUpNicknamePage />)

    const service = await screen.findByRole("region", { name: "(필수) 이용약관 동의" })
    expect(service).toHaveTextContent("제1조 (목적)")
    expect(service).toHaveTextContent("moa 이용약관 전문입니다.")
    expect(screen.getByRole("region", { name: "(필수) 개인정보 수집·이용 동의" })).toHaveTextContent("수집 항목")
    expect(screen.queryByRole("button", { name: "전문 보기" })).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "더보기" })).toHaveAttribute("target", "_blank")
    expect(screen.getByRole("link", { name: "개인정보처리방침 보기" })).toHaveAttribute("target", "_blank")
  })
})
