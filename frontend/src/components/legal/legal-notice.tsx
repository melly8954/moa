"use client"

import { Info } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { dateParts, legalPath } from "@/lib/legal"
import type { LegalDocumentKey } from "@/types/legal"

// 안내 띠 (specs/012-terms-privacy US3, contracts/screens.md "화면 구성" 2). 인쇄 때 숨긴다
//   upcoming: 지금 버전 화면 + 예정 버전 있음 → "YYYY년 M월 D일부터 바뀝니다" + 미리 보기
//   past: 예전 버전 화면 → "지금 시행 중인 버전이 아니에요" + 지금 버전 보기
//   future: 예정 버전 화면 → "YYYY년 M월 D일부터 시행돼요. 아직 시행 전이에요" + 지금 버전 보기
export type LegalNoticeKind =
  | { kind: "upcoming"; version: string; effectiveDate: string }
  | { kind: "past" }
  | { kind: "future"; effectiveDate: string }

export function LegalNotice({ document, notice }: { document: LegalDocumentKey; notice: LegalNoticeKind }) {
  const t = useTranslations("legal")
  const linkClass = "font-medium underline underline-offset-4"
  return (
    <Alert className="print:hidden">
      <Info aria-hidden />
      <AlertDescription className="flex flex-wrap items-center gap-x-2 gap-y-1">
        {notice.kind === "upcoming" && (
          <>
            <span>{t("noticeUpcoming", dateParts(notice.effectiveDate))}</span>
            <Link href={legalPath(document, notice.version)} className={linkClass}>
              {t("previewUpcoming")}
            </Link>
          </>
        )}
        {notice.kind === "past" && (
          <>
            <span>{t("noticePast")}</span>
            <Link href={legalPath(document)} className={linkClass}>
              {t("viewCurrent")}
            </Link>
          </>
        )}
        {notice.kind === "future" && (
          <>
            <span>{t("noticeFuture", dateParts(notice.effectiveDate))}</span>
            <Link href={legalPath(document)} className={linkClass}>
              {t("viewCurrent")}
            </Link>
          </>
        )}
      </AlertDescription>
    </Alert>
  )
}
