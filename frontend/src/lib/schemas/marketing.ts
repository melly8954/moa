import { z } from "zod"
import { isDateString } from "@/lib/date"
import { isAllowedLinkUrl, toOffsetDateTime } from "@/lib/marketing"

// 배너·팝업 폼이 같이 쓰는 칸 (specs/016-banner-popup FR-005·FR-007, research R5·R10)
//   연결 주소, 노출 기간(시작 날짜·시각 필수, 끝 날짜·시각 선택), 순서(0~9999, 기본 0), 켜짐(기본 켜짐)
export const exposureFields = {
  linkUrl: z.string().refine(isAllowedLinkUrl, "marketing.linkUrlInvalid"),
  startDate: z.string().refine(isDateString, "marketing.startRequired"),
  startTime: z.string().regex(/^\d{2}:\d{2}$/, "marketing.startRequired"),
  endDate: z.string(),
  endTime: z.string(),
  sortOrder: z.string().regex(/^\d{1,4}$/, "marketing.sortOrderInvalid"),
  enabled: z.boolean(),
}

export type ExposureInput = {
  linkUrl: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  sortOrder: string
  enabled: boolean
}

export const EXPOSURE_DEFAULTS: ExposureInput = {
  linkUrl: "",
  startDate: "",
  startTime: "",
  endDate: "",
  endTime: "",
  sortOrder: "0",
  enabled: true,
}

// 끝 일시는 날짜·시각을 함께 넣거나 함께 비운다. 넣으면 시작보다 뒤여야 한다
export function checkExposurePeriod(values: ExposureInput, ctx: z.RefinementCtx) {
  const hasEnd = values.endDate !== "" || values.endTime !== ""
  if (!hasEnd) return
  if (!isDateString(values.endDate) || !/^\d{2}:\d{2}$/.test(values.endTime)) {
    ctx.addIssue({ code: "custom", path: ["endDate"], message: "marketing.endIncomplete" })
    return
  }
  const start = `${values.startDate}T${values.startTime}`
  const end = `${values.endDate}T${values.endTime}`
  if (end <= start) ctx.addIssue({ code: "custom", path: ["endDate"], message: "marketing.endBeforeStart" })
}

// 폼 값 → 요청의 공통 칸
export function toExposureRequest(values: ExposureInput) {
  const hasEnd = values.endDate !== "" && values.endTime !== ""
  return {
    linkUrl: values.linkUrl === "" ? undefined : values.linkUrl,
    startsAt: toOffsetDateTime(values.startDate, values.startTime),
    endsAt: hasEnd ? toOffsetDateTime(values.endDate, values.endTime) : undefined,
    sortOrder: Number(values.sortOrder),
    enabled: values.enabled,
  }
}
