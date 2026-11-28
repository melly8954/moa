import { screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"
import ProfilePage from "@/app/(account)/account/profile/page"
import type { MyProfile, ProfileSaveRequest } from "@/types/profile"
import type { Me } from "@/types/user"
import { renderWithIntl } from "../intl"

// moa: 마이페이지 화면 (specs/009-mypage US1·US3~US4·US6). 서버는 정해 둔 값을 돌려준다
const server = vi.hoisted(() => ({
  get: vi.fn(),
  save: vi.fn(),
  changeNickname: vi.fn(),
  avatars: vi.fn(),
  codes: vi.fn(),
}))
let me: Partial<Me>
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))
vi.mock("@/lib/me", () => ({
  ME_QUERY_KEY: ["me"],
  useMe: () => ({ data: me, error: null, isPending: false, isError: false, refetch: vi.fn() }),
}))
vi.mock("@/api", () => ({
  api: {
    v1: {
      profile: {
        get: server.get,
        save: server.save,
        changeNickname: server.changeNickname,
        avatars: server.avatars,
      },
      code: { list: server.codes },
    },
  },
}))

const PROFILE: MyProfile = {
  nickname: "모아킴",
  nicknameTag: "0421",
  avatar: "default",
  ageGenderPublic: false,
  discordVisibility: "PRIVATE",
  playTimes: [],
}
const FILLED: MyProfile = {
  ...PROFILE,
  avatar: "cat",
  bio: "같이 해요",
  birthYear: 2000,
  gender: "FEMALE",
  ageGenderPublic: true,
  discordTag: "moa_user",
  discordVisibility: "MEMBERS",
  playTimes: [{ code: "DAWN", name: "새벽" }],
}

async function openPage(profile: MyProfile) {
  server.get.mockResolvedValue(profile)
  const view = renderWithIntl(<ProfilePage />)
  await screen.findByRole("button", { name: "저장" })
  await screen.findByRole("checkbox", { name: "평일 저녁" })
  return view
}

async function saved(): Promise<ProfileSaveRequest> {
  await userEvent.click(screen.getByRole("button", { name: "저장" }))
  await waitFor(() => expect(server.save).toHaveBeenCalledTimes(1))
  return server.save.mock.calls[0][0] as ProfileSaveRequest
}

describe("마이페이지 화면", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    me = { mypageAllowed: true }
    server.save.mockResolvedValue(PROFILE)
    server.avatars.mockResolvedValue({ avatars: ["default", "cat", "fox"] })
    server.codes.mockResolvedValue([
      { code: "WEEKDAY_EVENING", name: "평일 저녁" },
      { code: "DAWN", name: "새벽" },
    ])
  })

  it("US1-AC1 닉네임#태그와 적어 둔 항목이 보인다", async () => {
    await openPage(FILLED)

    expect(screen.getByText("모아킴#0421")).toBeInTheDocument()
    expect(screen.getByRole("img", { name: "고양이" })).toBeInTheDocument()
    expect(screen.getByLabelText(/^자기소개/)).toHaveValue("같이 해요")
    expect(screen.getByLabelText(/^출생 연도/)).toHaveValue("2000")
    expect(screen.getByText(`${new Date().getFullYear() - 2000}세`)).toBeInTheDocument()
    expect(screen.getByLabelText(/^성별/)).toHaveValue("FEMALE")
    expect(screen.getByLabelText(/^디스코드 사용자 이름/)).toHaveValue("moa_user")
    expect(screen.getByLabelText(/^디스코드 공개 범위/)).toHaveValue("MEMBERS")
    expect(screen.getByRole("checkbox", { name: "새벽" })).toBeChecked()
  })

  it("US1-AC1 적지 않은 항목은 비어 있음으로 보이고, 태그가 없으면 닉네임만 보인다", async () => {
    await openPage({ ...PROFILE, nicknameTag: undefined })

    expect(screen.getByText("모아킴")).toBeInTheDocument()
    for (const label of [/^자기소개/, /^출생 연도/, /^디스코드 사용자 이름/]) {
      expect(screen.getByLabelText(label)).toHaveValue("")
      expect(screen.getByLabelText(label)).toHaveAttribute("placeholder", "비어 있음")
    }
  })

  it("US1-AC5 로그인 방식과 소셜 계정 연결은 마이페이지에 없다", async () => {
    await openPage(PROFILE)

    expect(screen.queryByText(/비밀번호|구글|Google|소셜|연결/)).not.toBeInTheDocument()
  })

  it("US1-AC7 관리자·운영자 계정이면 권한 없음 안내만 보이고 프로필을 부르지 않는다", () => {
    me = { mypageAllowed: false }
    renderWithIntl(<ProfilePage />)

    expect(screen.getByRole("alert")).toHaveTextContent("관리자 계정은 마이페이지를 쓸 수 없어요.")
    expect(screen.queryByRole("button", { name: "저장" })).not.toBeInTheDocument()
    expect(server.get).not.toHaveBeenCalled()
  })

  it("US1-AC8 닉네임은 필수, 자기소개·나이·성별·디스코드 태그·시간대는 선택으로 표시된다", async () => {
    await openPage(PROFILE)

    expect(screen.getByText("닉네임", { selector: "label" })).toHaveTextContent("필수")
    for (const label of ["자기소개", "출생 연도", "성별", "디스코드 사용자 이름"])
      expect(screen.getByText(label, { selector: "label" })).toHaveTextContent("선택")
    expect(screen.getByText("플레이 가능 시간대", { selector: "legend" })).toHaveTextContent("선택")
    expect(screen.getAllByText("필수")).toHaveLength(1)
  })

  it("US3-AC5 적어 둔 값을 지우고 저장하면 빈 값(null)으로 보낸다", async () => {
    await openPage(FILLED)
    for (const label of [/^자기소개/, /^출생 연도/, /^디스코드 사용자 이름/])
      await userEvent.clear(screen.getByLabelText(label))
    await userEvent.selectOptions(screen.getByLabelText(/^성별/), "")

    expect(await saved()).toMatchObject({ bio: null, birthYear: null, gender: null, discordTag: null })
  })

  it("US4-AC1 목록에서 아바타를 고르면 머리 그림이 바뀌고 저장할 때 함께 보낸다", async () => {
    await openPage(PROFILE)
    await userEvent.click(screen.getByRole("button", { name: "아바타 고르기" }))
    await userEvent.click(await screen.findByRole("button", { name: "여우" }))

    expect(await screen.findByRole("img", { name: "여우" })).toBeInTheDocument()
    expect(await saved()).toMatchObject({ avatar: "fox" })
  })

  it("US4-AC2 아바타는 서버가 준 목록에서만 고르고 파일 올리기나 주소 입력이 없다", async () => {
    await openPage(PROFILE)
    await userEvent.click(screen.getByRole("button", { name: "아바타 고르기" }))
    const dialog = await screen.findByRole("dialog")

    expect(await within(dialog).findByRole("button", { name: "여우" })).toBeInTheDocument()
    expect(within(dialog).queryByRole("button", { name: "로봇" })).not.toBeInTheDocument()
    expect(within(dialog).queryByRole("textbox")).not.toBeInTheDocument()
    expect(document.querySelector('input[type="file"]')).toBeNull()
  })

  it("US6-AC1 고른 시간대를 저장할 때 보낸다", async () => {
    await openPage(PROFILE)
    await userEvent.click(screen.getByRole("checkbox", { name: "평일 저녁" }))

    expect(await saved()).toMatchObject({ playTimes: ["WEEKDAY_EVENING"] })
  })

  it("US6-AC2 시간대를 모두 풀고 저장하면 빈 목록으로 보낸다", async () => {
    await openPage(FILLED)
    await userEvent.click(screen.getByRole("checkbox", { name: "새벽" }))

    expect(await saved()).toMatchObject({ playTimes: [] })
  })

  it("Edge 공통코드에서 끈 시간대도 이미 고른 것은 이름과 함께 보인다", async () => {
    server.codes.mockResolvedValue([{ code: "WEEKDAY_EVENING", name: "평일 저녁" }])
    await openPage(FILLED)

    expect(screen.getByRole("checkbox", { name: "새벽" })).toBeChecked()
  })
})
