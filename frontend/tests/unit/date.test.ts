import { describe, expect, it } from "vitest"
import {
  addDays,
  addMonths,
  fromCalendarDate,
  inRange,
  isDateString,
  matchingQuickRange,
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

  // moa: 전체(빈 기간)·오늘·7일·이번 달·3개월, 전체 말고는 모두 오늘 포함 (specs/011-list-screen FR-011, US4-AC3)
  it("빠른 기간", () => {
    const now = new Date("2026-11-21T10:00:00+09:00")
    expect(quickRange("all", now)).toEqual({ from: "", to: "" })
    expect(quickRange("today", now)).toEqual({ from: "2026-11-21", to: "2026-11-21" })
    expect(quickRange("last7Days", now)).toEqual({ from: "2026-11-15", to: "2026-11-21" })
    expect(quickRange("thisMonth", now)).toEqual({ from: "2026-11-01", to: "2026-11-21" })
    expect(quickRange("last3Months", now)).toEqual({ from: "2026-08-22", to: "2026-11-21" })
    // 그 날짜가 없는 달이면 그 달 마지막 날의 다음 날부터
    expect(quickRange("last3Months", new Date("2026-05-31T10:00:00+09:00"))).toEqual({
      from: "2026-03-01",
      to: "2026-05-31",
    })
  })

  it("달 더하기는 달 끝에 맞춘다", () => {
    expect(addMonths("2026-05-31", -3)).toBe("2026-02-28")
    expect(addMonths("2026-01-31", 1)).toBe("2026-02-28")
    expect(addMonths("2026-11-21", -12)).toBe("2025-11-21")
  })

  it("지금 기간과 같은 빠른 기간을 찾는다", () => {
    const now = new Date("2026-11-21T10:00:00+09:00")
    expect(matchingQuickRange({ from: "2026-11-15", to: "2026-11-21" }, now)).toBe("last7Days")
    expect(matchingQuickRange({ from: "2026-11-10", to: "2026-11-21" }, now)).toBeUndefined()
    // 두 칸이 비면 전체, 한 칸만 차면 어느 단추도 아니다 (2026-11-22)
    expect(matchingQuickRange({ from: "", to: "" }, now)).toBe("all")
    expect(matchingQuickRange({ from: "2026-11-15", to: "" }, now)).toBeUndefined()
  })
})
