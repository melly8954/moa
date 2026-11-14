import { describe, expect, it } from "vitest"
import { noticeSchema } from "@/lib/schemas/notice"

// 공지 쓰기·고치기 폼 규칙 (specs/006-notices data-model.md, 백엔드 NoticeCreateRequest와 같다)
describe("공지 스키마", () => {
  const valid = { title: "점검 안내", content: "<p>본문</p>", pinned: false }

  it("제목과 본문이 있으면 통과한다", () => {
    expect(noticeSchema.safeParse(valid).success).toBe(true)
  })

  it("제목이 비었거나 공백만이면 막는다", () => {
    for (const title of ["", "   "]) {
      const result = noticeSchema.safeParse({ ...valid, title })
      expect(result.error?.issues.map((i) => i.message)).toEqual(["notice.titleRequired"])
    }
  })

  it("제목은 200자까지다", () => {
    expect(noticeSchema.safeParse({ ...valid, title: "가".repeat(200) }).success).toBe(true)
    expect(noticeSchema.safeParse({ ...valid, title: "가".repeat(201) }).error?.issues[0]?.message).toBe(
      "notice.titleMax",
    )
  })

  it("본문이 비면 막고, HTML 글자 수 20,000자까지다", () => {
    expect(noticeSchema.safeParse({ ...valid, content: "" }).error?.issues[0]?.message).toBe("notice.contentRequired")
    expect(noticeSchema.safeParse({ ...valid, content: "a".repeat(20000) }).success).toBe(true)
    expect(noticeSchema.safeParse({ ...valid, content: "a".repeat(20001) }).error?.issues[0]?.message).toBe(
      "notice.contentMax",
    )
  })
})
