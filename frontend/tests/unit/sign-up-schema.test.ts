import { describe, expect, it } from "vitest"
import { signUpSchema } from "@/lib/schemas/sign-up"

// moa: 소셜 가입 닉네임·약관 폼 규칙 (specs/010-social-login FR-005·FR-013, 잠정 닉네임 규칙)
const agreed = { agreeService: true, agreePrivacy: true }

function nicknameError(nickname: string) {
  const result = signUpSchema.safeParse({ nickname, ...agreed })
  return result.success ? undefined : result.error.issues.find((issue) => issue.path[0] === "nickname")?.message
}

describe("소셜 가입 폼 규칙", () => {
  it("US1-AC3 닉네임: 2~12자 한글·영문·숫자·_는 통과하고 앞뒤 공백은 지운다", () => {
    expect(nicknameError("모아")).toBeUndefined()
    expect(nicknameError("Moa_Kim_2026")).toBeUndefined()
    expect(signUpSchema.parse({ nickname: "  모아킴  ", ...agreed }).nickname).toBe("모아킴")
  })

  it("US1-AC3 닉네임: 비었거나 1자·13자·허용되지 않는 글자면 막는다", () => {
    expect(nicknameError("   ")).toBe("signUp.nicknameRequired")
    expect(nicknameError("a")).toBe("signUp.nicknameRule")
    expect(nicknameError("a".repeat(13))).toBe("signUp.nicknameRule")
    expect(nicknameError("모아!")).toBe("signUp.nicknameRule")
    expect(nicknameError("모 아")).toBe("signUp.nicknameRule")
  })

  it("US1-AC3 닉네임: 분해형 한글(NFD)도 완성형으로 바꿔 통과한다", () => {
    const decomposed = "모아킴".normalize("NFD")

    const result = signUpSchema.parse({ nickname: decomposed, ...agreed })

    expect(result.nickname).toBe("모아킴")
  })

  it("US1-AC7 필수 약관 둘 다 동의해야 통과한다", () => {
    expect(signUpSchema.safeParse({ nickname: "모아킴", agreeService: true, agreePrivacy: false }).success).toBe(false)
    expect(signUpSchema.safeParse({ nickname: "모아킴", agreeService: false, agreePrivacy: true }).success).toBe(false)
    expect(signUpSchema.safeParse({ nickname: "모아킴", ...agreed }).success).toBe(true)
  })
})
