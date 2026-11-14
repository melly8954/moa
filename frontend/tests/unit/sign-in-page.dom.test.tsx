import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import SignInPage from "@/app/(public)/sign-in/page"
import { renderWithIntl } from "../intl"

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: vi.fn() }) }))

describe("로그인 화면", () => {
  it("비운 채 제출하면 칸마다 오류를 보이고 서버에 보내지 않는다", async () => {
    renderWithIntl(<SignInPage />)
    await userEvent.click(screen.getByRole("button", { name: "로그인" }))

    expect(await screen.findByText("아이디를 입력해 주세요.")).toBeInTheDocument()
    expect(screen.getByText("비밀번호를 입력해 주세요.")).toBeInTheDocument()
    expect(screen.getByLabelText("아이디")).toHaveAttribute("aria-invalid", "true")
  })
})
