import { describe, expect, it } from "vitest"
import { isOnRelatedScreen, safeLink, unreadLabel } from "@/lib/notification"

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

describe("관련 화면을 보고 있으면 토스트를 띄우지 않는다 (spec FR-007, research R14)", () => {
  const at = (link: string) => ({ notificationType: "SYSTEM" as const, link })

  it("연결 화면과 지금 경로가 같으면 관련 화면이다", () => {
    expect(isOnRelatedScreen(at("/notices/7"), "/notices/7")).toBe(true)
    expect(isOnRelatedScreen(at("/"), "/")).toBe(true)
  })

  it("쿼리·#과 끝의 /는 보지 않는다", () => {
    expect(isOnRelatedScreen(at("/notifications?page=2"), "/notifications")).toBe(true)
    expect(isOnRelatedScreen(at("/notices/7#top"), "/notices/7/")).toBe(true)
  })

  it("다른 화면이면 관련 화면이 아니다", () => {
    expect(isOnRelatedScreen(at("/notices/7"), "/notices")).toBe(false)
    expect(isOnRelatedScreen(at("/notices/7"), "/notices/70")).toBe(false)
    expect(isOnRelatedScreen(at("//evil.example.com"), "/notices")).toBe(false)
  })
})
