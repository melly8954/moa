"use client"

import { useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { formatDateTime } from "@/lib/format"

// moa: 탈퇴한 뒤 로그인 화면의 안내 (specs/018-member-withdrawal US1-AC5, contracts/screens.md)
// 탈퇴 창이 /sign-in?withdrawnUntil=<완전 삭제 예정 시각>으로 보낸다. 쿼리가 없거나 시각이 아니면 아무것도 보이지 않는다
// useSearchParams를 쓰므로 부르는 쪽이 <Suspense>로 감싼다 (Next 16 정적 렌더링)
export function WithdrawalNotice() {
  const until = useSearchParams().get("withdrawnUntil")
  const t = useTranslations("signIn")
  if (!until || Number.isNaN(Date.parse(until))) return null
  return (
    <Alert role="status">
      <AlertDescription>{t("withdrawnNotice", { date: formatDateTime(until) })}</AlertDescription>
    </Alert>
  )
}
