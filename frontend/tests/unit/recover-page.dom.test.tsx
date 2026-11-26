import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { ApiError } from "@/api/error"
import SignInPage from "@/app/(public)/sign-in/page"
import RecoverPage from "@/app/(public)/sign-in/recover/page"
import { useAuthStore } from "@/stores/auth"
import { renderWithIntl } from "../intl"

// moa: 탈퇴한 계정 복구 화면과 로그인 화면의 A401 (specs/018-member-withdrawal US2-AC2·AC5, contracts/screens.md)
const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  recovery: vi.fn(),
  recover: vi.fn(),
  login: vi.fn(),
  landing: vi.fn(),
}))
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
  useSearchParams: () => new URLSearchParams(),
}))
vi.mock("@/components/shell/landing", () => ({ fetchLandingPath: mocks.landing }))
vi.mock("@/api", () => ({
  api: {
    v1: {
      withdrawal: { recovery: mocks.recovery, recover: mocks.recover },
      auth: { login: mocks.login },
      oauth: { providers: vi.fn().mockResolvedValue({ providers: [] }), authorizePath: vi.fn() },
    },
  },
}))

describe("복구 화면", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState(null, "", "/sign-in/recover")
    useAuthStore.getState().signOut()
    mocks.recovery.mockResolvedValue({ purgeAt: "2026-12-26T10:00:00+09:00" })
    mocks.landing.mockResolvedValue("/")
  })

  it("완전 삭제 예정 시각을 보인다", async () => {
    renderWithIntl(<RecoverPage />)

    expect(await screen.findByText(/2026-12-26 10:00에 계정과 개인정보가 완전히 삭제돼요/)).toBeInTheDocument()
  })

  it("US2-AC5 취소하면 복구하지 않고 로그인 화면으로 간다 (탈퇴 상태 그대로)", async () => {
    renderWithIntl(<RecoverPage />)
    await screen.findByText(/완전히 삭제돼요/)

    await userEvent.click(screen.getByRole("button", { name: "취소" }))

    expect(mocks.replace).toHaveBeenCalledWith("/sign-in")
    expect(mocks.recover).not.toHaveBeenCalled()
    expect(useAuthStore.getState().status).toBe("signedOut")
  })

  it("복구하면 로그인하고 돌아갈 주소(next)로 간다", async () => {
    window.history.replaceState(null, "", "/sign-in/recover?next=%2Fparties")
    mocks.recover.mockResolvedValue({ accessToken: "token", tokenType: "Bearer", expiresIn: 900 })
    renderWithIntl(<RecoverPage />)
    await screen.findByText(/완전히 삭제돼요/)

    await userEvent.click(screen.getByRole("button", { name: "복구하기" }))

    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/parties"))
    expect(useAuthStore.getState().accessToken).toBe("token")
  })

  it("복구 쿠키가 없거나 맞지 않으면(A403) 로그인 화면으로 보낸다", async () => {
    mocks.recovery.mockRejectedValue(new ApiError(400, "A403"))
    renderWithIntl(<RecoverPage />)

    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/sign-in"))
  })
})

describe("로그인 화면의 탈퇴 계정", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.history.replaceState(null, "", "/sign-in?next=%2Fparties")
  })

  it("US2-AC2 맞는 비밀번호로 탈퇴한 계정에 로그인하면(A401) 복구 화면으로 간다 (next를 이어 붙인다)", async () => {
    mocks.login.mockRejectedValue(new ApiError(401, "A401"))
    renderWithIntl(<SignInPage />)

    await userEvent.type(screen.getByLabelText("아이디"), "moa_kim")
    await userEvent.type(screen.getByLabelText("비밀번호"), "member1234!")
    await userEvent.click(screen.getByRole("button", { name: "로그인" }))

    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/sign-in/recover?next=%2Fparties"))
  })
})
