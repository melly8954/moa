import { z } from "zod"

// 공통코드 항목 드로어 폼. 규칙은 백엔드 CodeCreateRequest·CodeModifyRequest와 같다
//   코드: 비우면 자동 순번(moa, specs/004-admin-codes FR-006). 적으면 영문 대문자·숫자·밑줄, 50자까지 / 이름 100자까지
const CODE_PATTERN = /^[A-Z0-9_]*$/

export const codeItemSchema = z.object({
  code: z.string().trim().max(50, "code.codeMax").regex(CODE_PATTERN, "code.codePattern"),
  name: z.string().trim().min(1, "code.nameRequired").max(100, "code.nameMax"),
  enabled: z.boolean(),
})

export type CodeItemInput = z.input<typeof codeItemSchema>

// moa: 그룹 드로어 폼 (specs/004-admin-codes FR-004). 백엔드 CodeGroupCreateRequest·CodeGroupModifyRequest와 같다
//   그룹 코드: 영문 대문자로 시작, 대문자·숫자·밑줄 50자까지 (만들 때만) / 이름 100자 / 설명 500자 / 쓰는 곳 200자
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
  usedBy: z.string().trim().max(200, "code.usedByMax"),
})

export type CodeGroupInput = z.input<typeof codeGroupSchema>
