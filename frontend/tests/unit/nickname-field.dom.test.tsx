import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { ApiError } from "@/api/error"
import { NicknameField } from "@/components/profile/nickname-field"
import type { MyProfile } from "@/types/profile"
import { renderWithIntl } from "../intl"

// moa: 닉네임 바꾸기 칸 (specs/009-mypage US2, FR-003·FR-004)
const { changeNickname, success } = vi.hoisted(() => ({ changeNickname: vi.fn(), success: vi.fn() }))
vi.mock("sonner", () => ({ toast: { success } }))
vi.mock("@/lib/me", () => ({ ME_QUERY_KEY: ["me"] }))
vi.mock("@/api", () => ({ api: { v1: { profile: { changeNickname, get: vi.fn() } } } }))

const PROFILE: MyProfile = {
  nickname: "모아킴",
  nicknameTag: "0421",
  avatar: "default",
  ageGenderPublic: false,
  discordVisibility: "PRIVATE",
  playTimes: [],
}

function apiError(status: number, code: string, field?: string) {
  const errors = field ? [{ field, message: "서버 문구" }] : undefined
  return new ApiError(status, code, { code, message: "서버 문구", timestamp: "", path: "", errors })
}

async function submit(nickname: string) {
  const input = screen.getByLabelText(/^닉네임/)
  await userEvent.clear(input)
  if (nickname) await userEvent.type(input, nickname)
  await userEvent.click(screen.getByRole("button", { name: "닉네임 바꾸기" }))
}

describe("닉네임 바꾸기", () => {
  beforeEach(() => vi.clearAllMocks())

  it("US2-AC1 규칙에 맞는 새 닉네임을 보내고 바꿨다는 안내를 보인다", async () => {
    changeNickname.mockResolvedValue({ nickname: "새닉네임", nicknameTag: "0421" })
    renderWithIntl(<NicknameField profile={PROFILE} />)

    await submit("새닉네임")

    await waitFor(() => expect(success).toHaveBeenCalledWith("닉네임을 바꿨어요."))
    expect(changeNickname).toHaveBeenCalledWith("새닉네임")
  })

  it("US2-AC3 비우거나 규칙에 안 맞으면 보내지 않고 닉네임 칸에 이유가 보인다", async () => {
    renderWithIntl(<NicknameField profile={PROFILE} />)

    await submit("")
    expect(await screen.findByText("닉네임을 입력해 주세요.")).toBeInTheDocument()
    await submit("모아!")
    expect(await screen.findByText("2~12자의 한글, 영문, 숫자, _만 쓸 수 있어요.")).toBeInTheDocument()

    expect(changeNickname).not.toHaveBeenCalled()
  })

  it("US2-AC3 예약어는 서버가 막고 쓸 수 없는 닉네임이라고 보인다", async () => {
    changeNickname.mockRejectedValue(apiError(400, "V001", "nickname"))
    renderWithIntl(<NicknameField profile={PROFILE} />)

    await submit("admin")

    expect(await screen.findByText("쓸 수 없는 닉네임이에요. 다른 닉네임을 입력해 주세요.")).toBeInTheDocument()
  })

  it("US2-AC4 변경 제한 기간이면 언제부터 바꿀 수 있는지 보이고 입력과 단추가 꺼진다", () => {
    renderWithIntl(<NicknameField profile={{ ...PROFILE, nicknameChangeableAt: "2026-12-17T10:00:00+09:00" }} />)

    expect(screen.getByText("2026-12-17 10:00부터 바꿀 수 있어요")).toBeInTheDocument()
    expect(screen.getByLabelText(/^닉네임/)).toBeDisabled()
    expect(screen.getByRole("button", { name: "닉네임 바꾸기" })).toBeDisabled()
  })

  it("US2-AC6 변경 제한이 없으면(첫 변경) 바로 바꿀 수 있다", async () => {
    renderWithIntl(<NicknameField profile={PROFILE} />)
    await userEvent.type(screen.getByLabelText(/^닉네임/), "2")

    expect(screen.getByRole("button", { name: "닉네임 바꾸기" })).toBeEnabled()
  })

  it("US2-AC8 태그가 다 찬 닉네임(B201)이면 다른 닉네임을 고르라는 안내가 보인다", async () => {
    changeNickname.mockRejectedValue(apiError(409, "B201"))
    renderWithIntl(<NicknameField profile={PROFILE} />)

    await submit("busyname")

    expect(await screen.findByText("이 닉네임은 더 쓸 수 없어요. 다른 닉네임을 골라 주세요.")).toBeInTheDocument()
  })

  it("Edge 지금과 같은 닉네임이면 단추가 꺼져 있다", () => {
    renderWithIntl(<NicknameField profile={PROFILE} />)

    expect(screen.getByRole("button", { name: "닉네임 바꾸기" })).toBeDisabled()
  })
})
