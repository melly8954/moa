import { screen } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { OAuthErrorAlert } from "@/components/auth/oauth-error-alert"
import { renderWithIntl } from "../intl"

// moa: 로그인 화면의 소셜 로그인 실패 안내 (specs/010-social-login US3, US1-AC9·AC12, US2-AC2)
let search = ""
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(search) }))

describe("소셜 로그인 실패 안내", () => {
  beforeEach(() => {
    search = ""
  })

  it("US3-AC1 쿼리가 없으면(취소) 아무 안내도 보이지 않는다", () => {
    const { container } = renderWithIntl(<OAuthErrorAlert />)

    expect(container).toBeEmptyDOMElement()
  })

  it("US3-AC1 모르는 값이면 아무 안내도 보이지 않는다", () => {
    search = "oauthError=whatever"

    const { container } = renderWithIntl(<OAuthErrorAlert />)

    expect(container).toBeEmptyDOMElement()
  })

  it.each([
    ["US3-AC2", "failed", "소셜 로그인에 실패했어요. 다시 시도해 주세요."],
    ["US2-AC2", "locked", "잠긴 계정이에요. 관리자에게 문의해 주세요."],
    ["US3-AC3", "disabled", "지금은 이 방식으로 로그인할 수 없어요."],
    ["US1-AC12", "emailUnverified", "제공자에서 이메일 인증을 마친 뒤 다시 시도해 주세요."],
    ["US1-AC9", "emailInUse", "다른 방식으로 가입된 이메일이에요. 원래 방식으로 로그인해 주세요."],
  ])("%s oauthError=%s면 그 안내를 보인다", (_ac, code, message) => {
    search = `oauthError=${code}`

    renderWithIntl(<OAuthErrorAlert />)

    expect(screen.getByText(message)).toBeInTheDocument()
  })
})
