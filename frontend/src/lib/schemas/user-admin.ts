import { z } from "zod"

// 사용자 관리 폼. 규칙은 백엔드 UserCreateRequest·UserRenameRequest와 같다 (이름은 드로어 한 폼 userEditSchema)
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

// moa: 사용자 드로어 한 폼 (이름·계정 종류·역할, specs/002-admin-users FR-007, research R16, 2026-11-22)
export const userEditSchema = z.object({
  name,
  role: z.enum(["USER", "ADMIN"]),
  roleIds: z.array(z.number()),
})

export type UserCreateInput = z.infer<typeof userCreateSchema>
export type UserEditInput = z.infer<typeof userEditSchema>
