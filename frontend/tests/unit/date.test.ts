import { describe, expect, it } from "vitest"
import {
  addDays,
  fromCalendarDate,
  inRange,
  isDateString,
  parseTypedDate,
  quickRange,
  toCalendarDate,
  today,
} from "@/lib/date"

describe("날짜 문자열", () => {
  it("있는 날짜만 YYYY-MM-DD로 본다", () => {
    expect(isDateString("2026-11-06")).toBe(true)
    expect(isDateString("2026-02-30")).toBe(false)
    expect(isDateString("2026-1-6")).toBe(false)
  })

  it("달력과 주고받아도 하루가 밀리지 않는다", () => {
    expect(fromCalendarDate(toCalendarDate("2026-11-06"))).toBe("2026-11-06")
    expect(fromCalendarDate(new Date(2026, 0, 1, 0, 0))).toBe("2026-01-01")
    expect(fromCalendarDate(new Date(2026, 0, 1, 23, 59))).toBe("2026-01-01")
  })

  it("입력칸 글자를 여러 모양으로 읽는다", () => {
    expect(parseTypedDate("20261106")).toBe("2026-11-06")
    expect(parseTypedDate("2026.11.6")).toBe("2026-11-06")
    expect(parseTypedDate("2026/11/06")).toBe("2026-11-06")
    expect(parseTypedDate(" ")).toBe("")
    expect(parseTypedDate("2026-13-01")).toBeUndefined()
    expect(parseTypedDate("내일")).toBeUndefined()
  })

  it("오늘은 서울 날짜다", () => {
    expect(today(new Date("2026-11-06T16:00:00Z"))).toBe("2026-11-07")
  })

  it("날짜 더하기와 범위", () => {
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28")
    expect(inRange("2026-11-06", "2026-11-01", "2026-11-30")).toBe(true)
    expect(inRange("2026-12-01", undefined, "2026-11-30")).toBe(false)
  })

  it("빠른 선택", () => {
    const now = new Date("2026-11-06T10:00:00+09:00")
    expect(quickRange("today", now)).toEqual({ from: "2026-11-06", to: "2026-11-06" })
    expect(quickRange("last7Days", now)).toEqual({ from: "2026-10-31", to: "2026-11-06" })
    expect(quickRange("thisMonth", now)).toEqual({ from: "2026-11-01", to: "2026-11-06" })
    expect(quickRange("lastMonth", now)).toEqual({ from: "2026-10-01", to: "2026-10-31" })
    expect(quickRange("lastMonth", new Date("2026-01-15T10:00:00+09:00"))).toEqual({
      from: "2025-12-01",
      to: "2025-12-31",
    })
  })
})
