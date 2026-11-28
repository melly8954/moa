import { describe, expect, it } from "vitest"
import {
  emptyPartyInput,
  type PartyInput,
  partySchema,
  positionCapacity,
  toPartySaveRequest,
} from "@/lib/schemas/party"

// moa: 파티 만들기 폼 규칙 (specs/008-party FR-003, FR-022, US1-AC3, US10-AC1·AC2, T054)
const VALID: PartyInput = { ...emptyPartyInput, gameCode: "PUBG", title: "같이 해요" }

function firstIssue(values: Partial<PartyInput>) {
  const result = partySchema.safeParse({ ...VALID, ...values })
  return result.success ? undefined : result.error.issues[0]
}

describe("파티 만들기 폼 규칙", () => {
  it("US1-AC3 제목: 0자(공백만 포함)면 막고, 1자·50자는 통과하고, 51자면 막는다", () => {
    expect(firstIssue({ title: "" })?.message).toBe("party.create.titleRequired")
    expect(firstIssue({ title: "   " })?.message).toBe("party.create.titleRequired")
    expect(firstIssue({ title: "a" })).toBeUndefined()
    expect(firstIssue({ title: "가".repeat(50) })).toBeUndefined()
    expect(firstIssue({ title: "가".repeat(51) })?.message).toBe("party.create.titleMax")
  })

  it("US1-AC3 인원: 1명이면 막고, 2명·10명은 통과하고, 11명이면 막는다", () => {
    expect(firstIssue({ capacity: 1 })?.message).toBe("party.create.capacityRange")
    expect(firstIssue({ capacity: 2 })).toBeUndefined()
    expect(firstIssue({ capacity: 10 })).toBeUndefined()
    expect(firstIssue({ capacity: 11 })?.message).toBe("party.create.capacityRange")
  })

  it("US1-AC3 게임을 고르지 않으면 막는다", () => {
    expect(firstIssue({ gameCode: "" })?.message).toBe("party.create.gameRequired")
  })

  it("US10-AC1 롤: 방장 MID, 모집 JUNGLE·TOP이면 인원은 자리 수의 합 3이고 포지션마다 1자리다", () => {
    const values: PartyInput = {
      ...VALID,
      gameCode: "LOL",
      positionEnabled: true,
      hostPositionCode: "MID",
      // 롤은 포지션마다 1자리라 2를 줘도 1로 센다
      slots: { JUNGLE: 1, TOP: 2, ADC: 0 },
    }
    expect(firstIssue(values)).toBeUndefined()
    expect(positionCapacity(values)).toBe(3)
    const request = toPartySaveRequest(values)
    expect(request.capacity).toBe(3)
    expect(request.hostPositionCode).toBe("MID")
    expect(request.positions).toEqual(
      expect.arrayContaining([
        { positionCode: "MID", slots: 1 },
        { positionCode: "JUNGLE", slots: 1 },
        { positionCode: "TOP", slots: 1 },
      ]),
    )
    expect(request.positions).toHaveLength(3)
  })

  it("US10-AC2 오버워치: TANK 1(방장), DAMAGE 2, SUPPORT 2면 인원은 5다. 폼의 인원 칸 값은 쓰지 않는다", () => {
    const values: PartyInput = {
      ...VALID,
      gameCode: "OVERWATCH",
      capacity: 10,
      positionEnabled: true,
      hostPositionCode: "TANK",
      slots: { TANK: 1, DAMAGE: 2, SUPPORT: 2 },
    }
    expect(firstIssue(values)).toBeUndefined()
    expect(toPartySaveRequest(values).capacity).toBe(5)
  })

  it("포지션 조건을 켜면: 방장 포지션이 없으면 막고, 자리 수의 합이 2~10명 밖이면 막는다", () => {
    const base: Partial<PartyInput> = { gameCode: "OVERWATCH", positionEnabled: true }
    expect(firstIssue({ ...base, hostPositionCode: "", slots: { DAMAGE: 2 } })?.message).toBe(
      "party.create.hostPositionRequired",
    )
    // 방장 자리 1개뿐
    expect(firstIssue({ ...base, hostPositionCode: "TANK", slots: {} })?.message).toBe("party.create.slotsRange")
    // 2명, 10명은 통과
    expect(firstIssue({ ...base, hostPositionCode: "TANK", slots: { DAMAGE: 1 } })).toBeUndefined()
    expect(firstIssue({ ...base, hostPositionCode: "TANK", slots: { TANK: 2, DAMAGE: 4, SUPPORT: 4 } })).toBeUndefined()
    // 11명
    expect(firstIssue({ ...base, hostPositionCode: "TANK", slots: { TANK: 3, DAMAGE: 4, SUPPORT: 4 } })?.path).toEqual([
      "slots",
    ])
  })

  it("포지션이 없는 게임(배그)은 조건을 켜도 포지션을 보내지 않고 고른 인원을 쓴다", () => {
    const request = toPartySaveRequest({
      ...VALID,
      gameCode: "PUBG",
      capacity: 4,
      positionEnabled: true,
      slots: { TOP: 1 },
    })
    expect(request.capacity).toBe(4)
    expect(request.positions).toBeUndefined()
    expect(request.hostPositionCode).toBeUndefined()
  })
})
