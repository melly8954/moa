import { z } from "zod"
import { NICKNAME_PATTERN } from "./sign-up"

// moa: 마이페이지 폼 (specs/009-mypage FR-003·FR-006~FR-008, research R6·R7)
// 화면은 길이·형식만 먼저 막는다. 예약어·태그·변경 제한·목록 확인은 서버 응답(V001·B201·B202)을 따른다
// 오류 문구는 messages/ko.json의 키로 적는다

// 닉네임 규칙(잠정)은 010 가입과 같다 (백엔드 NicknamePolicy)
export const nicknameSchema = z.object({
  nickname: z
    .string()
    .normalize("NFC")
    .trim()
    .min(1, "profile.nicknameRequired")
    .regex(NICKNAME_PATTERN, "profile.nicknameRule"),
})

export type NicknameInput = z.infer<typeof nicknameSchema>

export const BIO_MAX = 500
export const MIN_BIRTH_YEAR = 1900
// 디스코드 사용자 이름: 소문자·숫자·_·. 2~32자, 마침표 연속 불가. 대문자는 바꾸지 않고 형식 오류 (백엔드 ProfileService)
export const DISCORD_TAG_PATTERN = /^(?!.*\.\.)[a-z0-9_.]{2,32}$/

// 출생 연도는 "" 또는 1900~올해 (미래 연도 불가). 문구에 범위를 끼우므로 화면이 키를 보고 값을 넣는다
export const BIRTH_YEAR_RANGE = "profile.birthYearRange"

export function profileSchema(thisYear: number = new Date().getFullYear()) {
  return z.object({
    avatar: z.string(),
    bio: z.string().max(BIO_MAX, "profile.bioMax"),
    birthYear: z
      .string()
      .trim()
      .refine((v) => v === "" || (/^\d{4}$/.test(v) && Number(v) >= MIN_BIRTH_YEAR && Number(v) <= thisYear), {
        message: BIRTH_YEAR_RANGE,
      }),
    gender: z.enum(["", "MALE", "FEMALE", "UNDISCLOSED"]),
    ageGenderPublic: z.boolean(),
    discordTag: z
      .string()
      .trim()
      .refine((v) => v === "" || DISCORD_TAG_PATTERN.test(v), { message: "profile.discordTagFormat" }),
    discordVisibility: z.enum(["PRIVATE", "FRIENDS", "MEMBERS"]),
    playTimes: z.array(z.string()),
  })
}

export type ProfileInput = z.infer<ReturnType<typeof profileSchema>>
