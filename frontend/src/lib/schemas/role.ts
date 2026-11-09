import { z } from "zod"

// 역할 폼. 규칙은 백엔드 RoleCreateRequest·RoleModifyRequest와 같다
//   코드: 영문 대문자로 시작, 대문자·숫자·밑줄 50자까지 / 이름 50자까지 / 설명 200자까지
export const roleSchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "role.codeRequired")
    .regex(/^[A-Z][A-Z0-9_]{0,49}$/, "role.codePattern"),
  name: z.string().trim().min(1, "role.nameRequired").max(50, "role.nameMax"),
  description: z.string().trim().max(200, "role.descriptionMax"),
  permissions: z.array(z.string()),
})

export type RoleInput = z.infer<typeof roleSchema>
