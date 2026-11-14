import { describe, expect, it } from "vitest"
import { safeLink, unreadLabel } from "@/lib/notification"

// 알림 표시 규칙 (specs/007-notifications contracts/notifications-screens.md)
describe("알림 벨 숫자", () => {
  it("0이면 비우고, 99까지는 그대로, 넘으면 99+", () => {
    expect(unreadLabel(0)).toBe("")
    expect(unreadLabel(3)).toBe("3")
    expect(unreadLabel(99)).toBe("99")
    expect(unreadLabel(100)).toBe("99+")
  })
})

describe("알림 링크", () => {
  it("moa 화면 경로만 열고, 다른 사이트 주소는 첫 화면으로 바꾼다", () => {
    expect(safeLink("/notices/7")).toBe("/notices/7")
    expect(safeLink("//evil.example.com")).toBe("/")
    expect(safeLink("https://evil.example.com")).toBe("/")
  })
})
