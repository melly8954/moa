import { describe, expect, it } from "vitest"
import { signInSchema } from "@/lib/schemas/sign-in"

describe("로그인 스키마", () => {
  it("아이디는 앞뒤 공백을 자르고, 비밀번호는 자르지 않는다", () => {
    expect(signInSchema.parse({ loginId: "  user ", password: " pw " })).toEqual({ loginId: "user", password: " pw " })
  })

  it("공백만인 값은 비었다고 본다", () => {
    const result = signInSchema.safeParse({ loginId: "   ", password: "   " })
    expect(result.success).toBe(false)
    expect(result.error?.issues.map((i) => i.path[0])).toEqual(["loginId", "password"])
  })
})
