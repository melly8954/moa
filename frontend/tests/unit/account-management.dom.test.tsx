import { screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { ApiError } from "@/api/error"
import AccountManagePage from "@/app/(account)/account/profile/manage/page"
import type { AccountManagementItem } from "@/components/account-management/items"
import { MypageFrame } from "@/components/profile/mypage-frame"
import { renderWithIntl } from "../intl"

// moa: 계정 관리 탭과 탈퇴 확인 창 (specs/018-member-withdrawal US1-AC1·AC2·AC3·AC5·AC9, US4-AC1·AC2)
const mocks = vi.hoisted(() => ({
  withdraw: vi.fn(),
  unsubscribeBrowserOnly: vi.fn(),
  items: [] as AccountManagementItem[],
}))
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn(), replace: vi.fn() }) }))
vi.mock("@/lib/me", () => ({
  ME_QUERY_KEY: ["me"],
  useMe: () => ({
    data: { name: "MoaKim", mypageAllowed: true },
    error: null,
    isPending: false,
    isError: false,
    refetch: vi.fn(),
  }),
}))
vi.mock("@/api", () => ({ api: { v1: { withdrawal: { withdraw: mocks.withdraw } } } }))
vi.mock("@/lib/web-push", () => ({ unsubscribeBrowserOnly: mocks.unsubscribeBrowserOnly }))
vi.mock("@/components/account-management/items", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/components/account-management/items")>()
  return {
    ...actual,
    get ACCOUNT_MANAGEMENT_ITEMS() {
      return mocks.items
    },
  }
})

const replace = vi.fn()
const originalLocation = window.location

function openDialog() {
  return userEvent.click(screen.getByRole("button", { name: "탈퇴하기" }))
}

function confirmButton() {
  return within(screen.getByRole("alertdialog")).getByRole("button", { name: "탈퇴하기" })
}

describe("계정 관리 탭", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.items = []
    mocks.unsubscribeBrowserOnly.mockResolvedValue(undefined)
    Object.defineProperty(window, "location", { configurable: true, value: { ...originalLocation, replace } })
  })

  afterEach(() => {
    Object.defineProperty(window, "location", { configurable: true, value: originalLocation })
  })

  it("US1-AC1 마이페이지에 프로필·계정 관리 탭이 있고 처음은 프로필이다. 계정 관리 맨 아래 오른쪽에 탈퇴하기가 있다", () => {
    const { unmount } = renderWithIntl(
      <MypageFrame tab="profile">
        <p>내용</p>
      </MypageFrame>,
    )
    expect(screen.getByRole("link", { name: "프로필" })).toHaveAttribute("aria-current", "page")
    expect(screen.getByRole("link", { name: "계정 관리" })).toHaveAttribute("href", "/account/profile/manage")
    unmount()

    renderWithIntl(<AccountManagePage />)
    expect(screen.getByRole("link", { name: "계정 관리" })).toHaveAttribute("aria-current", "page")
    expect(screen.getByRole("button", { name: "탈퇴하기" }).parentElement).toHaveClass("justify-end")
  })

  it("US1-AC2 탈퇴하기를 누르면 30일 복구, 완전 삭제, 남는 글의 안내가 보이고 아직 탈퇴되지 않았다", async () => {
    renderWithIntl(<AccountManagePage />)
    await openDialog()

    const dialog = screen.getByRole("alertdialog")
    expect(within(dialog).getByText(/30일 안에 다시 로그인하면/)).toBeInTheDocument()
    expect(within(dialog).getByText(/완전히 삭제되고 되돌릴 수 없어요/)).toBeInTheDocument()
    expect(within(dialog).getByText(/"탈퇴한 회원"으로 보여요\. 남기고 싶지 않으면/)).toBeInTheDocument()
    expect(mocks.withdraw).not.toHaveBeenCalled()
  })

  it("US1-AC3 닉네임이 비었거나 다르면(대소문자 포함) 탈퇴 단추를 쓸 수 없고, 앞뒤 공백만 다르면 쓸 수 있다", async () => {
    renderWithIntl(<AccountManagePage />)
    await openDialog()
    const input = screen.getByLabelText("닉네임")

    expect(confirmButton()).toBeDisabled()
    await userEvent.type(input, "moakim")
    expect(confirmButton()).toBeDisabled()
    await userEvent.clear(input)
    await userEvent.type(input, "  MoaKim ")
    expect(confirmButton()).toBeEnabled()
  })

  it("US1-AC9 취소하면 탈퇴하지 않고, 다시 열면 입력이 비어 있다", async () => {
    renderWithIntl(<AccountManagePage />)
    await openDialog()
    await userEvent.type(screen.getByLabelText("닉네임"), "MoaKim")

    await userEvent.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "취소" }))
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument())
    await openDialog()

    expect(screen.getByLabelText("닉네임")).toHaveValue("")
    expect(mocks.withdraw).not.toHaveBeenCalled()
  })

  it("서버가 닉네임이 다르다고 하면(V001) 칸 아래에 알리고 창을 닫지 않는다", async () => {
    mocks.withdraw.mockRejectedValue(
      new ApiError(400, "V001", {
        code: "V001",
        message: "입력값이 올바르지 않습니다.",
        errors: [{ field: "nickname", reason: "x" }],
      } as never),
    )
    renderWithIntl(<AccountManagePage />)
    await openDialog()
    await userEvent.type(screen.getByLabelText("닉네임"), "MoaKim")
    await userEvent.click(confirmButton())

    expect(await screen.findByText("닉네임이 달라요.")).toBeInTheDocument()
    expect(screen.getByRole("alertdialog")).toBeInTheDocument()
    expect(replace).not.toHaveBeenCalled()
  })

  it("US1-AC5 탈퇴하면 브라우저 구독만 해제하고 복구 기한을 붙인 로그인 화면을 새로 연다", async () => {
    mocks.withdraw.mockResolvedValue({ purgeAt: "2026-12-26T10:00:00+09:00" })
    renderWithIntl(<AccountManagePage />)
    await openDialog()
    await userEvent.type(screen.getByLabelText("닉네임"), "MoaKim")
    await userEvent.click(confirmButton())

    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith("/sign-in?withdrawnUntil=2026-12-26T10%3A00%3A00%2B09%3A00"),
    )
    expect(mocks.withdraw).toHaveBeenCalledWith("MoaKim")
    expect(mocks.unsubscribeBrowserOnly).toHaveBeenCalled()
  })

  it("US4-AC1 더해진 항목은 정해진 순서로 보이고 탈퇴하기는 여전히 맨 아래다", () => {
    mocks.items = [
      { key: "second", order: 20, Component: () => <p>둘째 항목</p> },
      { key: "first", order: 10, Component: () => <p>첫째 항목</p> },
    ]
    renderWithIntl(<AccountManagePage />)

    const first = screen.getByText("첫째 항목")
    const second = screen.getByText("둘째 항목")
    const button = screen.getByRole("button", { name: "탈퇴하기" })
    expect(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(second.compareDocumentPosition(button) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it("US4-AC2 더해진 항목이 없으면 빈 자리 없이 탈퇴하기 영역만 있다", () => {
    renderWithIntl(<AccountManagePage />)

    const section = screen.getByRole("button", { name: "탈퇴하기" }).parentElement
    expect(section?.parentElement?.children).toHaveLength(1)
  })
})
