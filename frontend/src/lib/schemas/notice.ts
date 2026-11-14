import { z } from "zod"

// 공지 쓰기·고치기. 규칙은 백엔드 NoticeCreateRequest(@NotBlank, @Size)와 같다
//   제목: 1~200자 / 본문: HTML 글자 수 20,000자까지 (서버는 거른 뒤의 값으로 다시 본다)
export const noticeSchema = z.object({
  title: z
    .string()
    .refine((v) => v.trim().length > 0, "notice.titleRequired")
    .refine((v) => v.length <= 200, "notice.titleMax"),
  content: z.string().min(1, "notice.contentRequired").max(20000, "notice.contentMax"),
  pinned: z.boolean(),
})

export type NoticeInput = z.infer<typeof noticeSchema>
