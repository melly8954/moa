import { z } from "zod"
import { checkExposurePeriod, exposureFields } from "@/lib/schemas/marketing"

// 팝업 드로어 (specs/016-banner-popup FR-004). 규칙은 백엔드 PopupSaveRequest와 같다
//   제목 1~200자, 내용 HTML 20,000자까지 (서버는 거른 뒤의 값으로 다시 본다)
export const popupSchema = z
  .object({
    title: z
      .string()
      .refine((v) => v.trim().length > 0, "popup.titleRequired")
      .refine((v) => v.length <= 200, "popup.titleMax"),
    content: z.string().min(1, "popup.contentRequired").max(20000, "popup.contentMax"),
    ...exposureFields,
  })
  .superRefine(checkExposurePeriod)

export type PopupInput = z.infer<typeof popupSchema>
