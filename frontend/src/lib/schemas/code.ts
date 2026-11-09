import { z } from "zod"

// 코드 관리 폼. 규칙은 백엔드 CodeGroupCreateRequest·CodeCreateRequest·CodeModifyRequest와 같다
//   그룹 코드·코드: 영문 대문자·숫자·밑줄, 50자까지 / 이름 100자까지 / 설명 500자까지 / 정렬 순서 0 이상
const CODE_PATTERN = /^[A-Z0-9_]+$/

const codeValue = z
  .string()
  .trim()
  .min(1, "code.codeRequired")
  .max(50, "code.codeMax")
  .regex(CODE_PATTERN, "code.codePattern")
const name = z.string().trim().min(1, "code.nameRequired").max(100, "code.nameMax")
// 입력칸 값(문자열)을 0 이상 정수로
const sortOrder = z
  .string()
  .trim()
  .regex(/^\d+$/, "code.sortOrderInvalid")
  .transform((v) => Number(v))

export const codeGroupSchema = z.object({
  groupCode: codeValue,
  name,
  description: z.string().trim().max(500, "code.descriptionMax"),
})

export const codeCreateSchema = z.object({
  code: codeValue,
  name,
  sortOrder,
  enabled: z.boolean(),
})

export type CodeGroupInput = z.input<typeof codeGroupSchema>
export type CodeFormInput = z.input<typeof codeCreateSchema>
export type CodeFormOutput = z.output<typeof codeCreateSchema>
