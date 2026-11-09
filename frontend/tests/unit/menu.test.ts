import { describe, expect, it } from "vitest"
import { type MenuEntry, screenTitle, toMenuItems, visibleMenu } from "@/components/shell/menu"

// 메뉴 label은 messages/ko.json의 menu 키다. 테스트는 아무 키나 써도 되게 형을 느슨히 둔다
const entry = (label: string, rest: Partial<MenuEntry> = {}) => ({ label, ...rest }) as MenuEntry

const menu = [
  entry("home", { href: "/" }),
  entry("admin", {
    children: [
      entry("codes", { href: "/admin/codes", permission: "CODE_MANAGE" }),
      entry("roles", { href: "/admin/roles", permission: "ROLE_MANAGE" }),
    ],
  }),
  entry("report", { href: "/report", permission: "REPORT_VIEW" }),
]

describe("권한으로 메뉴 거르기", () => {
  it("권한이 있는 메뉴만 남긴다", () => {
    const visible = visibleMenu(menu, (key) => key === "CODE_MANAGE")
    expect(visible.map((m) => m.label)).toEqual(["home", "admin"])
    expect(visible[1].children?.map((m) => m.label)).toEqual(["codes"])
  })

  it("하위 메뉴가 모두 빠진 묶음은 뺀다", () => {
    expect(visibleMenu(menu, () => false).map((m) => m.label)).toEqual(["home"])
  })
})

describe("셸 메뉴로 바꾸기", () => {
  const items = toMenuItems(menu, (label) => `[${label}]`)

  it("key는 정의의 label, label은 번역한 글자", () => {
    expect(items[1]).toMatchObject({ key: "admin", label: "[admin]" })
    expect(items[1].children?.[0]).toMatchObject({ key: "codes", label: "[codes]", href: "/admin/codes" })
  })

  it("헤더 위치는 가장 깊이 맞는 메뉴", () => {
    expect(screenTitle(items, "/admin/codes/1")?.label).toBe("[codes]")
    expect(screenTitle(items, "/nowhere")).toBeUndefined()
  })
})
