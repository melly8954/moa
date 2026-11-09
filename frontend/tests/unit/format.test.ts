import { describe, expect, it } from "vitest"
import {
  EMPTY,
  formatBytes,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatNumber,
  formatRelative,
} from "@/lib/format"

// 시각은 SITE.timeZone(Asia/Seoul)으로 보인다. 테스트를 돌리는 컴퓨터 시간대와 상관없이 결과가 같아야 한다
describe("형식", () => {
  it("시각은 서울 시간대로, 연도까지 YYYY-MM-DD HH:mm (24시간)", () => {
    expect(formatDateTime("2026-11-06T05:30:05Z")).toBe("2026-11-06 14:30")
    expect(formatDateTime("2026-11-06T14:30:05+09:00", { seconds: true })).toBe("2026-11-06 14:30:05")
    expect(formatDateTime("2026-11-06T15:00:00Z")).toBe("2026-11-07 00:00")
  })

  it("한 곳만 다른 시간대로 보일 수 있다", () => {
    expect(formatDateTime("2026-11-06T14:30:00+09:00", { timeZone: "UTC" })).toBe("2026-11-06 05:30")
  })

  it("날짜: 시각은 서울 날짜로, 날짜만 있는 값은 그대로", () => {
    expect(formatDate("2026-11-06T20:00:00Z")).toBe("2026-11-07")
    expect(formatDate("2026-11-06")).toBe("2026-11-06")
  })

  it("값이 없거나 읽지 못하면 EMPTY", () => {
    for (const value of [null, undefined, "", "not-a-date"]) {
      expect(formatDate(value)).toBe(EMPTY)
      expect(formatDateTime(value)).toBe(EMPTY)
    }
    expect(formatNumber(null)).toBe(EMPTY)
    expect(formatNumber(Number.NaN)).toBe(EMPTY)
    expect(formatCurrency(undefined)).toBe(EMPTY)
  })

  it("숫자는 세 자리마다 쉼표", () => {
    expect(formatNumber(1234567)).toBe("1,234,567")
    expect(formatNumber(1234.567)).toBe("1,234.57")
    expect(formatNumber(0)).toBe("0")
  })

  it("화폐는 기본 원화", () => {
    expect(formatCurrency(12000)).toBe("₩12,000")
    expect(formatCurrency(10, "USD")).toBe("US$10.00")
  })

  it("지난 시각·남은 시각", () => {
    const now = new Date("2026-11-06T14:30:00+09:00")
    expect(formatRelative("2026-11-06T14:27:00+09:00", "ko", now)).toBe("3분 전")
    expect(formatRelative("2026-11-08T14:30:00+09:00", "ko", now)).toBe("모레")
  })

  it("파일 크기", () => {
    expect(formatBytes(512)).toBe("512 B")
    expect(formatBytes(1536)).toBe("1.5 KB")
    expect(formatBytes(5 * 1024 * 1024)).toBe("5 MB")
  })
})
