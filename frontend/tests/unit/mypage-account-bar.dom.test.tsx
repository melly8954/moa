import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { AccountBar } from "@/components/shell/account-bar"
import type { Me } from "@/types/user"
import { renderWithIntl } from "../intl"

// moa: 계정 바의 아바타·닉네임과 톱니 메뉴의 마이페이지 (specs/009-mypage FR-014·FR-016, US1-AC3·AC6, US4-AC1)
let me: Me | undefined
vi.mock("@/lib/me", () => ({ useMe: () => ({ data: me }) }))
vi.mock("@/components/shell/use-sign-out", () => ({ useSignOut: () => vi.fn() }))
vi.mock("@/components/ui/sidebar", () => ({ useSidebar: () => ({ state: "expanded", isMobile: false }) }))
vi.mock("@/components/notification/notification-bell", () => ({ NotificationBell: () => null }))

const MEMBER: Me = {
  id: 10,
  loginId: "moa_kim",
  name: "모아킴",
  role: "USER",
  passwordChangeRequired: false,
  hasPassword: true,
  avatar: "cat",
  mypageAllowed: true,
}

async function openPreferences() {
  await userEvent.click(screen.getByRole("button", { name: "설정 메뉴" }))
  await screen.findByRole("menuitem", { name: "설정" })
}

describe("계정 바와 마이페이지", () => {
  beforeEach(() => {
    me = MEMBER
  })

  it("US1-AC3 회원에게는 닉네임이 보이고 로그인 아이디는 보이지 않는다", () => {
    renderWithIntl(<AccountBar />)

    expect(screen.getAllByText("모아킴").length).toBeGreaterThan(0)
    expect(screen.queryByText("moa_kim")).not.toBeInTheDocument()
  })

  it("US4-AC1 고른 아바타가 계정 바에 보이고, 모르는 키면 기본 아바타가 보인다", () => {
    const { container, unmount } = renderWithIntl(<AccountBar />)
    expect(container.querySelector("img")).toHaveAttribute("src", "/avatars/cat.svg")
    unmount()

    me = { ...MEMBER, avatar: "removed" }
    expect(renderWithIntl(<AccountBar />).container.querySelector("img")).toHaveAttribute("src", "/avatars/default.svg")
  })

  it("US1-AC1 회원의 톱니 메뉴에는 마이페이지가 있고 마이페이지로 간다", async () => {
    renderWithIntl(<AccountBar />)
    await openPreferences()

    expect(screen.getByRole("menuitem", { name: "마이페이지" })).toHaveAttribute("href", "/account/profile")
  })

  it("US1-AC6 관리자·운영자 계정(mypageAllowed 거짓)의 톱니 메뉴에는 마이페이지가 없다", async () => {
    me = { ...MEMBER, role: "ADMIN", mypageAllowed: false }
    renderWithIntl(<AccountBar />)
    await openPreferences()

    expect(screen.queryByRole("menuitem", { name: "마이페이지" })).not.toBeInTheDocument()
  })
})
