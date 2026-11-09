import { z } from "zod"

// 내 비밀번호 바꾸기. 규칙은 백엔드 PasswordChangeRequest(@NotBlank, @Size)와 같다
//   지금 비밀번호: 100자까지 / 새 비밀번호: 8~100자, 지금과 달라야 한다 (서버도 V001로 막는다)
// 비밀번호는 자르지 않는다 (공백도 글자다)
export const passwordChangeSchema = z
  .object({
    currentPassword: z
      .string()
      .refine((v) => v.trim().length > 0, "password.currentRequired")
      .refine((v) => v.length <= 100, "password.max"),
    newPassword: z
      .string()
      .refine((v) => v.trim().length > 0, "password.newRequired")
      .refine((v) => v.length >= 8, "password.newMin")
      .refine((v) => v.length <= 100, "password.max"),
    newPasswordConfirm: z.string(),
  })
  .refine((v) => v.newPassword === v.newPasswordConfirm, {
    message: "password.confirmMismatch",
    path: ["newPasswordConfirm"],
  })
  .refine((v) => v.currentPassword === "" || v.newPassword !== v.currentPassword, {
    message: "password.sameAsCurrent",
    path: ["newPassword"],
  })

export type PasswordChangeInput = z.infer<typeof passwordChangeSchema>
