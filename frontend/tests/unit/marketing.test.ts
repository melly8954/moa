import { describe, expect, it } from "vitest"
import { fromOffsetDateTime, isAllowedLinkUrl, periodText, toOffsetDateTime } from "@/lib/marketing"

// 배너·팝업 관리가 같이 쓰는 규칙 (specs/016-banner-popup research R5, R10)
describe("isAllowedLinkUrl", () => {
  it("비우거나 내부 주소·http·https 주소는 받는다", () => {
    expect(isAllowedLinkUrl("")).toBe(true)
    expect(isAllowedLinkUrl("/notices/1")).toBe(true)
    expect(isAllowedLinkUrl("https://moa.example/event")).toBe(true)
    expect(isAllowedLinkUrl("http://moa.example")).toBe(true)
  })

  it("다른 사이트로 가는 //, javascript:, ftp:, 빈칸, 500자를 넘는 주소는 막는다", () => {
    expect(isAllowedLinkUrl("//evil.example")).toBe(false)
    expect(isAllowedLinkUrl("javascript:alert(1)")).toBe(false)
    expect(isAllowedLinkUrl("ftp://x")).toBe(false)
    expect(isAllowedLinkUrl("/a b")).toBe(false)
    expect(isAllowedLinkUrl(`/${"a".repeat(500)}`)).toBe(false)
  })
})

describe("노출 일시", () => {
  it("날짜·시각을 한국 시간 일시로 만들고 되돌린다", () => {
    const value = toOffsetDateTime("2026-12-01", "10:00")
    expect(value).toBe("2026-12-01T10:00:00+09:00")
    expect(fromOffsetDateTime(value)).toEqual({ date: "2026-12-01", time: "10:00" })
  })

  it("다른 시간대의 일시도 한국 시간으로 푼다", () => {
    expect(fromOffsetDateTime("2026-12-01T01:00:00Z")).toEqual({ date: "2026-12-01", time: "10:00" })
    expect(fromOffsetDateTime(null)).toEqual({ date: "", time: "" })
  })

  it("기간은 시작 ~ 끝, 끝이 없으면 시작 ~ 로 보인다", () => {
    expect(periodText("2026-12-01T10:00:00+09:00", "2026-12-10T23:59:00+09:00")).toBe(
      "2026-12-01 10:00 ~ 2026-12-10 23:59",
    )
    expect(periodText("2026-12-01T10:00:00+09:00", null)).toBe("2026-12-01 10:00 ~")
  })
})
