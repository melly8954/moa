import { z } from "zod"

// FAQ 드로어 (specs/017-customer-center FR-004). 규칙은 백엔드 FaqSaveRequest와 같다
//   분류 필수, 질문 1~200자, 답 HTML 20,000자까지 (서버는 거른 뒤의 값으로 다시 본다), 순서 0~9999(기본 0), 켜짐(기본 켜짐)
export const faqSchema = z.object({
  categoryCode: z.string().min(1, "faq.categoryRequired"),
  question: z
    .string()
    .refine((v) => v.trim().length > 0, "faq.questionRequired")
    .refine((v) => v.length <= 200, "faq.questionMax"),
  answer: z.string().min(1, "faq.answerRequired").max(20000, "faq.answerMax"),
  sortOrder: z.string().regex(/^\d{1,4}$/, "faq.sortOrderInvalid"),
  enabled: z.boolean(),
})

export type FaqInput = z.infer<typeof faqSchema>
