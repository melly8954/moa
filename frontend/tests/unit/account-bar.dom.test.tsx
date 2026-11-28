import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { AccountBar } from "@/components/shell/account-bar"
import type { Me } from "@/types/user"
import { renderWithIntl } from "../intl"

// moa: 계정 바의 아이디 줄과 톱니 메뉴의 비밀번호 변경 (specs/010-social-login US2-AC3, specs/009-mypage US1-AC3)
let me: Me | undefined
vi.mock("@/lib/me", () => ({ useMe: () => ({ data: me }) }))
vi.mock("@/components/shell/use-sign-out", () => ({ useSignOut: () => vi.fn() }))
vi.mock("@/components/ui/sidebar", () => ({ useSidebar: () => ({ state: "expanded", isMobile: false }) }))
// 알림 벨은 안 읽은 수를 서버에서 읽는다. 이 테스트는 계정 줄만 본다
vi.mock("@/components/notification/notification-bell", () => ({ NotificationBell: () => null }))

function member(loginId: string, hasPassword: boolean, mypageAllowed = true): Me {
  return {
    id: 10,
    loginId,
    name: "모아킴",
    role: mypageAllowed ? "USER" : "ADMIN",
    passwordChangeRequired: false,
    hasPassword,
    avatar: "default",
    mypageAllowed,
  }
}

async function openPreferences() {
  renderWithIntl(<AccountBar />)
  await userEvent.click(screen.getByRole("button", { name: "설정 메뉴" }))
}

describe("계정 바", () => {
  it("US2-AC3 비밀번호 없는 소셜 회원에게는 아이디 줄과 비밀번호 변경이 보이지 않는다", async () => {
    me = member("s_abc", false)

    await openPreferences()

    expect(screen.getAllByText("모아킴").length).toBeGreaterThan(0)
    expect(screen.queryByText("s_abc")).not.toBeInTheDocument()
    expect(await screen.findByRole("menuitem", { name: "설정" })).toBeInTheDocument()
    expect(screen.queryByRole("menuitem", { name: "비밀번호 변경" })).not.toBeInTheDocument()
  })

  it("009 US1-AC3 비밀번호가 있는 회원에게는 비밀번호 변경이 보이고, 아이디 줄은 보이지 않는다", async () => {
    me = member("moa_kim", true)

    await openPreferences()

    expect(screen.getAllByText("모아킴").length).toBeGreaterThan(0)
    expect(screen.queryByText("moa_kim")).not.toBeInTheDocument()
    expect(await screen.findByRole("menuitem", { name: "비밀번호 변경" })).toBeInTheDocument()
  })

  it("관리자 계정(mypageAllowed 거짓)에게는 아이디 줄과 비밀번호 변경이 보인다", async () => {
    me = member("admin_kim", true, false)

    await openPreferences()

    expect(screen.getAllByText("admin_kim").length).toBeGreaterThan(0)
    expect(await screen.findByRole("menuitem", { name: "비밀번호 변경" })).toBeInTheDocument()
  })
})
