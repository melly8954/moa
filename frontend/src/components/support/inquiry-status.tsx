"use client"

import { useTranslations } from "next-intl"
import { StatusText } from "@/components/kit/status-text"
import type { InquiryStatus } from "@/types/inquiry"

// 문의 상태 (specs/017-customer-center FR-008): 답변 대기는 보통, 답변 완료는 긍정
export function InquiryStatusText({ status }: { status: InquiryStatus }) {
  const t = useTranslations("inquiry")
  return <StatusText tone={status === "ANSWERED" ? "positive" : "neutral"}>{t(`statuses.${status}`)}</StatusText>
}
