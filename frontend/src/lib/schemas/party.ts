import { z } from "zod"
import type { JoinPolicy, PartySaveRequest } from "@/types/party"

// 파티 만들기·고치기 (specs/008-party FR-003, FR-022, spec.md 화면 "파티 만들기")
//   규칙은 백엔드 PartyCreateRequest와 같다: 제목 1~50자, 소개 1000자까지, 인원 2~10명
//   포지션 조건을 켜면 인원은 자리 수의 합(방장 자리 포함)이고 직접 고르지 않는다

export const JOIN_POLICIES: JoinPolicy[] = ["OPEN", "APPROVAL", "PRIVATE"]
export const CAPACITY_MIN = 2
export const CAPACITY_MAX = 10

// 포지션이 있는 게임 (FR-022). single: 포지션마다 1자리(롤), multi: 포지션마다 자리 수를 정한다(발로란트·오버워치)
// 배그처럼 여기 없는 게임은 포지션 칸이 없다
export type PositionKind = "single" | "multi"
const POSITION_KINDS: Record<string, PositionKind> = { LOL: "single", VALORANT: "multi", OVERWATCH: "multi" }
export const positionKindOf = (gameCode: string): PositionKind | undefined => POSITION_KINDS[gameCode]

// 공통코드 그룹 이름 규칙 (research.md R7)
export const modeGroupOf = (gameCode: string) => `${gameCode}_MODE`
export const positionGroupOf = (gameCode: string) => `${gameCode}_POSITION`
export const tierGroupOf = (gameCode: string) => `${gameCode}_TIER`

export const partySchema = z
  .object({
    gameCode: z.string().min(1, "party.create.gameRequired"),
    modeCode: z.string(),
    title: z
      .string()
      .refine((v) => v.trim().length > 0, "party.create.titleRequired")
      .refine((v) => v.length <= 50, "party.create.titleMax"),
    description: z.string().max(1000, "party.create.descriptionMax"),
    joinPolicy: z.enum(["OPEN", "APPROVAL", "PRIVATE"]),
    capacity: z
      .number()
      .int()
      .min(CAPACITY_MIN, "party.create.capacityRange")
      .max(CAPACITY_MAX, "party.create.capacityRange"),
    tierMinCode: z.string(),
    tierMaxCode: z.string(),
    positionEnabled: z.boolean(),
    hostPositionCode: z.string(),
    // 포지션 코드 → 자리 수 (방장 자리 포함). 롤은 0 또는 1
    slots: z.record(z.string(), z.number().int().min(0)),
  })
  .superRefine((v, ctx) => {
    if (!v.positionEnabled || !positionKindOf(v.gameCode)) return
    if (!v.hostPositionCode) {
      ctx.addIssue({ code: "custom", path: ["hostPositionCode"], message: "party.create.hostPositionRequired" })
      return
    }
    const total = positionCapacity(v)
    if (total < CAPACITY_MIN || total > CAPACITY_MAX)
      ctx.addIssue({ code: "custom", path: ["slots"], message: "party.create.slotsRange" })
  })

export type PartyInput = z.infer<typeof partySchema>

export const emptyPartyInput: PartyInput = {
  gameCode: "",
  modeCode: "",
  title: "",
  description: "",
  joinPolicy: "OPEN",
  capacity: 4,
  tierMinCode: "",
  tierMaxCode: "",
  positionEnabled: false,
  hostPositionCode: "",
  slots: {},
}

// 포지션 자리 수. 방장 포지션은 적어도 1자리로 센다 (롤은 방장 자리 1 + 모집할 포지션마다 1)
export function positionSlots(v: Pick<PartyInput, "gameCode" | "hostPositionCode" | "slots">): Record<string, number> {
  const single = positionKindOf(v.gameCode) === "single"
  const result: Record<string, number> = {}
  for (const [code, n] of Object.entries(v.slots)) {
    const slots = single ? Math.min(n, 1) : n
    if (slots > 0) result[code] = slots
  }
  if (v.hostPositionCode) result[v.hostPositionCode] = Math.max(result[v.hostPositionCode] ?? 0, 1)
  return result
}

export function positionCapacity(v: Pick<PartyInput, "gameCode" | "hostPositionCode" | "slots">): number {
  return Object.values(positionSlots(v)).reduce((sum, n) => sum + n, 0)
}

// 폼 값 → 요청. 빈 칸은 보내지 않는다
export function toPartySaveRequest(v: PartyInput): PartySaveRequest {
  const withPositions = v.positionEnabled && !!positionKindOf(v.gameCode)
  const slots = withPositions ? positionSlots(v) : {}
  return {
    gameCode: v.gameCode,
    modeCode: v.modeCode || undefined,
    title: v.title.trim(),
    description: v.description.trim() || undefined,
    capacity: withPositions ? positionCapacity(v) : v.capacity,
    joinPolicy: v.joinPolicy,
    tierMinCode: v.tierMinCode || undefined,
    tierMaxCode: v.tierMaxCode || undefined,
    positions: withPositions
      ? Object.entries(slots).map(([positionCode, n]) => ({ positionCode, slots: n }))
      : undefined,
    hostPositionCode: withPositions ? v.hostPositionCode : undefined,
  }
}
