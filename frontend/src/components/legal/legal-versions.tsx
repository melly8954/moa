"use client"

import { ChevronDown } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { legalPath } from "@/lib/legal"
import type { LegalDocumentKey, VersionState } from "@/types/legal"

// 버전 목록 (specs/012-terms-privacy US3-AC1, contracts/screens.md "화면 구성" 4)
//   "다른 버전 보기"를 누르면 펼친다. 시행일 최근순(API 순서), 예정·지금 시행 중 표시, 줄마다 그 버전 화면 링크
//   보고 있는 버전은 aria-current
export function LegalVersions({
  document,
  versions,
  viewing,
}: {
  document: LegalDocumentKey
  versions: { version: string; effectiveDate: string; state: VersionState }[]
  viewing: string
}) {
  const t = useTranslations("legal")
  const [open, setOpen] = useState(false)
  const listId = `legal-versions-${document}`
  return (
    <div className="flex flex-col gap-2 print:hidden">
      <button
        type="button"
        className="inline-flex w-fit items-center gap-1 text-sm font-medium underline-offset-4 hover:underline"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        {t("otherVersions")}
        <ChevronDown
          aria-hidden
          className="size-4 transition-transform data-open:rotate-180"
          data-open={open || undefined}
        />
      </button>
      {open && (
        <ul id={listId} className="flex flex-col divide-y rounded-md border text-sm">
          {versions.map((item) => (
            <li key={item.version}>
              <Link
                href={legalPath(document, item.version)}
                aria-current={item.version === viewing ? "page" : undefined}
                className="flex flex-wrap items-center gap-2 px-3 py-2 hover:bg-muted aria-[current=page]:font-semibold"
              >
                <span>{t("effectiveOn", { date: item.effectiveDate })}</span>
                <span className="text-muted-foreground">{t("versionName", { version: item.version })}</span>
                {item.state !== "PAST" && (
                  <span className="rounded-sm border px-1.5 text-xs">
                    {item.state === "CURRENT" ? t("stateCurrent") : t("stateUpcoming")}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
