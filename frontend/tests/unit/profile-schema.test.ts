import { describe, expect, it } from "vitest"
import { nicknameSchema, profileSchema } from "@/lib/schemas/profile"

// moa: 마이페이지 폼 규칙 (specs/009-mypage FR-003·FR-006~FR-008, US2·US3)
function nicknameError(nickname: string) {
  const result = nicknameSchema.safeParse({ nickname })
  return result.success ? undefined : result.error.issues[0]?.message
}

const EMPTY = {
  avatar: "default",
  bio: "",
  birthYear: "",
  gender: "",
  ageGenderPublic: false,
  discordTag: "",
  discordVisibility: "PRIVATE",
  playTimes: [],
}

function profileError(values: Partial<Record<keyof typeof EMPTY, unknown>>) {
  const result = profileSchema(2026).safeParse({ ...EMPTY, ...values })
  return result.success ? undefined : result.error.issues[0]
}

describe("마이페이지 폼 규칙", () => {
  it("US2-AC3 닉네임: 비었거나 공백·1자·13자·허용되지 않는 글자면 막는다", () => {
    expect(nicknameError("")).toBe("profile.nicknameRequired")
    expect(nicknameError("   ")).toBe("profile.nicknameRequired")
    expect(nicknameError("가")).toBe("profile.nicknameRule")
    expect(nicknameError("a".repeat(13))).toBe("profile.nicknameRule")
    expect(nicknameError("모아!")).toBe("profile.nicknameRule")
    expect(nicknameError("모 아")).toBe("profile.nicknameRule")
  })

  it("US2-AC3 닉네임: 규칙에 맞으면 통과하고 앞뒤 공백은 지운다", () => {
    expect(nicknameError("모아")).toBeUndefined()
    expect(nicknameSchema.parse({ nickname: "  Moa_Kim_2026  " }).nickname).toBe("Moa_Kim_2026")
  })

  it("US3-AC1 자기소개: 500자까지 통과하고 501자면 막는다", () => {
    expect(profileError({ bio: "가".repeat(500) })).toBeUndefined()
    expect(profileError({ bio: "가".repeat(501) })?.message).toBe("profile.bioMax")
  })

  it("US3-AC2 출생 연도: 비었거나 1900~올해면 통과하고, 미래·1900년 전·숫자가 아니면 막는다", () => {
    for (const birthYear of ["", "1900", "2026"]) expect(profileError({ birthYear })).toBeUndefined()
    for (const birthYear of ["2027", "1899", "200", "이천년"])
      expect(profileError({ birthYear })?.path).toEqual(["birthYear"])
  })

  it("US3-AC3 디스코드 태그: 새 사용자명 형식과 빈 값은 통과한다", () => {
    for (const discordTag of ["", "ab", "moa_user.01", "a".repeat(32)])
      expect(profileError({ discordTag })).toBeUndefined()
  })

  it("US3-AC4 디스코드 태그: 예전 형식·대문자·허용되지 않는 글자·마침표 연속·길이 밖이면 막는다", () => {
    for (const discordTag of ["name#1234", "Moa", "모아유저", "moa user", "moa..user", "a", "a".repeat(33)])
      expect(profileError({ discordTag })?.message).toBe("profile.discordTagFormat")
  })
})
