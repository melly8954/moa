import { z } from "zod"

// moa: 소셜 가입 닉네임·약관 폼 (specs/010-social-login FR-005·FR-013)
// 닉네임 규칙(잠정)은 백엔드 NicknamePolicy와 같다: NFC → 앞뒤 공백 제거 → 2~12자 한글·영문·숫자·_
// 예약어는 서버가 막는다(V001 칸 오류). 다른 회원과 같은 닉네임은 막지 않는다 (태그로 구별)
// 오류 문구는 messages/ko.json의 키로 적는다. 화면이 fieldErrorText()로 바꿔 보인다
// 완성형 한글 범위(U+AC00~U+D7A3)는 이스케이프로 적는다 (문구 검사가 한글 글자를 막는다)
export const NICKNAME_PATTERN = /^[\uAC00-\uD7A3A-Za-z0-9_]{2,12}$/

export const signUpSchema = z.object({
  nickname: z
    .string()
    .normalize("NFC")
    .trim()
    .min(1, "signUp.nicknameRequired")
    .regex(NICKNAME_PATTERN, "signUp.nicknameRule"),
  agreeService: z.boolean().refine((v) => v, "signUp.termsRequired"),
  agreePrivacy: z.boolean().refine((v) => v, "signUp.termsRequired"),
})

export type SignUpInput = z.infer<typeof signUpSchema>
