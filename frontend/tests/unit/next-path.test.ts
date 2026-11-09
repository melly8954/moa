import { describe, expect, it } from "vitest"
import { safeNextPath, signInPath } from "@/lib/next-path"

describe("로그인 뒤 돌아갈 주소", () => {
  it("같은 사이트 경로만 받는다", () => {
    expect(safeNextPath("/orders?page=2")).toBe("/orders?page=2")
    for (const value of [null, undefined, "", "orders", "https://evil.example", "//evil.example", "/\\evil.example"]) {
      expect(safeNextPath(value)).toBeUndefined()
    }
  })

  it("로그인 화면으로 돌아가지 않는다", () => {
    expect(safeNextPath("/sign-in")).toBeUndefined()
    expect(safeNextPath("/sign-in?next=/x")).toBeUndefined()
  })

  it("로그인 화면 주소에 next를 붙인다 (첫 화면이면 붙이지 않는다)", () => {
    expect(signInPath("/orders?page=2")).toBe("/sign-in?next=%2Forders%3Fpage%3D2")
    expect(signInPath("/")).toBe("/sign-in")
    expect(signInPath()).toBe("/sign-in")
    expect(signInPath("https://evil.example")).toBe("/sign-in")
  })
})
