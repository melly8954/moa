import { describe, expect, it } from "vitest"
import { parseTime } from "@/components/kit/time-input"

describe("24시간 시각 칸", () => {
  it("여러 모양을 HH:mm으로 맞춘다", () => {
    expect(parseTime("930")).toBe("09:30")
    expect(parseTime("9:30")).toBe("09:30")
    expect(parseTime("0930")).toBe("09:30")
    expect(parseTime("23:59")).toBe("23:59")
    expect(parseTime("")).toBe("")
  })

  it("없는 시각은 읽지 않는다", () => {
    expect(parseTime("24:00")).toBeUndefined()
    expect(parseTime("12:60")).toBeUndefined()
    expect(parseTime("오후 2시")).toBeUndefined()
  })
})
