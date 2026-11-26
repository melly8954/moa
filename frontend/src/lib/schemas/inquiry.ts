import { z } from "zod"

// 1:1 문의 (specs/017-customer-center FR-007, FR-008). 규칙은 백엔드 InquiryCreateRequest·InquiryAnswerRequest와 같다
export const INQUIRY_TITLE_MAX = 100
export const INQUIRY_CONTENT_MAX = 2000
export const INQUIRY_IMAGE_MAX = 5

export const inquirySchema = z.object({
  categoryCode: z.string().min(1, "inquiry.categoryRequired"),
  title: z
    .string()
    .refine((v) => v.trim().length > 0, "inquiry.titleRequired")
    .refine((v) => v.length <= INQUIRY_TITLE_MAX, "inquiry.titleMax"),
  content: z
    .string()
    .refine((v) => v.trim().length > 0, "inquiry.contentRequired")
    .refine((v) => v.length <= INQUIRY_CONTENT_MAX, "inquiry.contentMax"),
  imageUrls: z.array(z.string()).max(INQUIRY_IMAGE_MAX, "inquiry.imageMax"),
})

export type InquiryInput = z.infer<typeof inquirySchema>

// 관리자 답변 (서식 있는 글, 20,000자까지)
export const answerSchema = z.object({
  content: z.string().min(1, "inquiry.answerRequired").max(20000, "inquiry.answerMax"),
})

export type AnswerInput = z.infer<typeof answerSchema>
