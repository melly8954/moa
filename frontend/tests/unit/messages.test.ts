import { describe, expect, it } from "vitest"
import { passwordChangeSchema } from "@/lib/schemas/password"
import { signInSchema } from "@/lib/schemas/sign-in"
import ko from "../../messages/ko.json"

function has(key: string): boolean {
  let node: unknown = ko
  for (const part of key.split(".")) {
    if (typeof node !== "object" || node === null || !(part in node)) return false
    node = (node as Record<string, unknown>)[part]
  }
  return typeof node === "string"
}

// Zod 스키마의 오류 문구는 messages/ko.json 키다. 키가 틀리면 화면에 키가 그대로 보인다
describe("스키마 오류 문구 키", () => {
  it("로그인 스키마", () => {
    const empty = signInSchema.safeParse({ loginId: "", password: "" })
    const long = signInSchema.safeParse({ loginId: "a".repeat(51), password: "a".repeat(101) })
    const messages = [...(empty.error?.issues ?? []), ...(long.error?.issues ?? [])].map((i) => i.message)
    expect(messages).toHaveLength(4)
    for (const message of messages) expect(has(message), message).toBe(true)
  })

  it("비밀번호 변경 스키마", () => {
    const cases = [
      { currentPassword: "", newPassword: "", newPasswordConfirm: "x" },
      { currentPassword: "a".repeat(101), newPassword: "short", newPasswordConfirm: "short" },
      { currentPassword: "same-pass", newPassword: "same-pass", newPasswordConfirm: "same-pass" },
    ]
    const messages = cases.flatMap((c) => passwordChangeSchema.safeParse(c).error?.issues ?? []).map((i) => i.message)
    expect(new Set(messages)).toEqual(
      new Set([
        "password.currentRequired",
        "password.newRequired",
        "password.newMin",
        "password.confirmMismatch",
        "password.max",
        "password.sameAsCurrent",
      ]),
    )
    for (const message of messages) expect(has(message), message).toBe(true)
  })
})
