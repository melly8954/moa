import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { AccountMenu } from "@/components/shell/account-menu"
import type { Me } from "@/types/user"
import { renderWithIntl } from "../intl"

// moa: 계정 메뉴의 비밀번호 변경 (specs/010-social-login US2-AC3)
let me: Me | undefined
vi.mock("@/lib/me", () => ({ useMe: () => ({ data: me }) }))
vi.mock("@/components/shell/use-sign-out", () => ({ useSignOut: () => vi.fn() }))

function member(hasPassword: boolean): Me {
  return { id: 10, loginId: "s_abc", name: "모아킴", role: "USER", passwordChangeRequired: false, hasPassword }
}

async function openMenu() {
  renderWithIntl(<AccountMenu trigger={<button type="button" />} />)
  await userEvent.click(screen.getByRole("button", { name: "모아킴" }))
}

describe("계정 메뉴", () => {
  it("US2-AC3 비밀번호 없는 소셜 회원에게는 비밀번호 변경이 보이지 않는다", async () => {
    me = member(false)

    await openMenu()

    expect(await screen.findByRole("menuitem", { name: "설정" })).toBeInTheDocument()
    expect(screen.queryByRole("menuitem", { name: "비밀번호 변경" })).not.toBeInTheDocument()
  })

  it("US2-AC3 비밀번호가 있는 회원에게는 비밀번호 변경이 보인다", async () => {
    me = member(true)

    await openMenu()

    expect(await screen.findByRole("menuitem", { name: "비밀번호 변경" })).toBeInTheDocument()
  })
})
