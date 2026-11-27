import { z } from "zod"
import { checkExposurePeriod, exposureFields } from "@/lib/schemas/marketing"

// 배너 드로어 (specs/016-banner-popup FR-003). 규칙은 백엔드 BannerSaveRequest와 같다
//   이름 1~100자, 넓은 화면 이미지 필수, 좁은 화면 이미지 선택, 대체 문구 1~200자
export const bannerSchema = z
  .object({
    name: z
      .string()
      .refine((v) => v.trim().length > 0, "banner.nameRequired")
      .refine((v) => v.length <= 100, "banner.nameMax"),
    wideImageUrl: z.string().min(1, "banner.wideImageRequired"),
    narrowImageUrl: z.string(),
    altText: z
      .string()
      .refine((v) => v.trim().length > 0, "banner.altTextRequired")
      .refine((v) => v.length <= 200, "banner.altTextMax"),
    ...exposureFields,
  })
  .superRefine(checkExposurePeriod)

export type BannerInput = z.infer<typeof bannerSchema>
