"use client"

import { useTranslations } from "next-intl"
import { StatusText, type StatusTone } from "@/components/kit/status-text"
import type { ExposureStatus } from "@/types/marketing"

// 배너·팝업 상태 (specs/016-banner-popup FR-006). 노출 중은 긍정, 예정은 보통, 끝남·꺼짐은 흐림
const TONE: Record<ExposureStatus, StatusTone> = {
  ACTIVE: "positive",
  SCHEDULED: "neutral",
  ENDED: "neutral",
  DISABLED: "neutral",
}

export function ExposureStatusText({ status }: { status: ExposureStatus }) {
  const t = useTranslations("marketing")
  const faded = status === "ENDED" || status === "DISABLED"
  return (
    <StatusText tone={TONE[status]} className={faded ? "text-muted-foreground" : undefined}>
      {t(`statuses.${status}`)}
    </StatusText>
  )
}
