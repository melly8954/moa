import { z } from "zod"

// 공통코드 항목 표의 추가 줄. 규칙은 백엔드 CodeCreateRequest와 같다
//   코드: 필수(moa, 자동 순번 없음, specs/004-admin-codes FR-006 2026-11-21), 영문 대문자·숫자·밑줄 50자까지 / 이름 100자까지
const CODE_PATTERN = /^[A-Z0-9_]+$/

export const codeItemSchema = z.object({
  code: z.string().trim().min(1, "code.codeRequired").max(50, "code.codeMax").regex(CODE_PATTERN, "code.codePattern"),
  name: z.string().trim().min(1, "code.nameRequired").max(100, "code.nameMax"),
})

export type CodeItemInput = z.input<typeof codeItemSchema>

// moa: 그룹 드로어 폼 (specs/004-admin-codes FR-004). 백엔드 CodeGroupCreateRequest·CodeGroupModifyRequest와 같다
//   그룹 코드: 영문 대문자로 시작, 대문자·숫자·밑줄 50자까지 (만들 때만) / 이름 100자 / 설명 500자
const GROUP_CODE_PATTERN = /^[A-Z][A-Z0-9_]*$/

export const codeGroupSchema = z.object({
  groupCode: z
    .string()
    .trim()
    .min(1, "code.groupCodeRequired")
    .max(50, "code.codeMax")
    .regex(GROUP_CODE_PATTERN, "code.groupCodePattern"),
  name: z.string().trim().min(1, "code.nameRequired").max(100, "code.nameMax"),
  description: z.string().trim().max(500, "code.descriptionMax"),
  enabled: z.boolean(),
})

export type CodeGroupInput = z.input<typeof codeGroupSchema>
