import { z } from "zod"

// 로그인 폼. 규칙은 백엔드 AuthLoginRequest(@NotBlank, @Size)와 같다
// 텍스트 칸은 앞뒤 공백을 잘라 보내고, 비밀번호는 자르지 않는다 (공백도 글자다)
// 오류 문구는 messages/ko.json의 키로 적는다. 화면이 fieldErrorText()로 바꿔 보인다
export const signInSchema = z.object({
  loginId: z.string().trim().min(1, "signIn.loginIdRequired").max(50, "signIn.loginIdMax"),
  password: z
    .string()
    .refine((v) => v.trim().length > 0, "signIn.passwordRequired")
    .refine((v) => v.length <= 100, "signIn.passwordMax"),
})

export type SignInInput = z.infer<typeof signInSchema>
