import { screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import SignInPage from "@/app/(public)/sign-in/page"
import { renderWithIntl } from "../intl"

// moa: 로그인 화면의 소셜 로그인 단추 (specs/010-social-login FR-001, US3-AC3)
const providers = vi.fn()
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}))
vi.mock("@/api", () => ({
  api: {
    v1: {
      auth: { login: vi.fn() },
      oauth: {
        providers: () => providers(),
        authorizePath: (provider: string) => `/api/v1/auth/oauth/${provider}/authorize`,
      },
    },
  },
}))

describe("로그인 화면의 소셜 로그인 단추", () => {
  beforeEach(() => {
    providers.mockReset()
  })

  it("US1-AC1 구글이 설정돼 있으면 Google로 계속하기 단추가 보이고 아이디 폼도 그대로다", async () => {
    providers.mockResolvedValue({ providers: ["google"] })

    renderWithIntl(<SignInPage />)

    expect(await screen.findByRole("button", { name: "Google로 계속하기" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "로그인" })).toBeInTheDocument()
  })

  it("US3-AC3 설정된 제공자가 없으면 소셜 단추가 없고 아이디 폼만 보인다", async () => {
    providers.mockResolvedValue({ providers: [] })

    renderWithIntl(<SignInPage />)

    await waitFor(() => expect(providers).toHaveBeenCalled())
    expect(screen.queryByRole("button", { name: "Google로 계속하기" })).not.toBeInTheDocument()
    expect(screen.getByRole("button", { name: "로그인" })).toBeInTheDocument()
  })
})
