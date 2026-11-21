import { describe, expect, it } from "vitest"
import { firstMenuPath, landingPath } from "@/components/shell/landing"
import type { MyMenu } from "@/types/menu"

// 로그인 뒤 첫 화면 (specs/001-admin-menus FR-006, research R6·R28)
const menu = (id: number, path: string | null, children: MyMenu[] = []): MyMenu => ({
  id,
  name: `메뉴 ${id}`,
  path,
  children,
})

describe("로그인 뒤 첫 화면", () => {
  // moa (2026-11-21): 관리 메뉴가 있는지가 아니라 계정 종류로 고른다. 역할 없는 관리자 계정도 /admin에서 안내를 본다
  it("관리자 계정(ADMIN)이면 /admin, 회원(USER)이나 모르면 /", () => {
    expect(landingPath("ADMIN")).toBe("/admin")
    expect(landingPath("USER")).toBe("/")
    expect(landingPath(undefined)).toBe("/")
  })
})

describe("첫 관리 메뉴", () => {
  it("위에서부터 처음 만나는 주소 있는 메뉴, 묶음이면 그 안을 먼저 본다", () => {
    expect(firstMenuPath([menu(1, "/admin/users"), menu(2, "/admin/menus")])).toBe("/admin/users")
    expect(firstMenuPath([menu(1, null, [menu(2, null), menu(3, "/admin/codes")]), menu(4, "/admin/roles")])).toBe(
      "/admin/codes",
    )
  })

  it("주소 있는 메뉴가 없으면 없다", () => {
    expect(firstMenuPath([])).toBeUndefined()
    expect(firstMenuPath([menu(1, null)])).toBeUndefined()
  })
})
