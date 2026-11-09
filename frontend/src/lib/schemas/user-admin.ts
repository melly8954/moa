import { z } from "zod"

// 사용자 관리 폼. 규칙은 백엔드 UserCreateRequest·UserRenameRequest와 같다
//   아이디: 영문·숫자·점·밑줄·하이픈 3~50자 / 이름: 50자까지
const name = z.string().trim().min(1, "userAdmin.nameRequired").max(50, "userAdmin.nameMax")

export const userCreateSchema = z.object({
  loginId: z
    .string()
    .trim()
    .min(1, "userAdmin.loginIdRequired")
    .regex(/^[A-Za-z0-9._-]{3,50}$/, "userAdmin.loginIdPattern"),
  name,
  role: z.enum(["USER", "ADMIN"]),
})

export const userRenameSchema = z.object({ name })

export type UserCreateInput = z.infer<typeof userCreateSchema>
export type UserRenameInput = z.infer<typeof userRenameSchema>
