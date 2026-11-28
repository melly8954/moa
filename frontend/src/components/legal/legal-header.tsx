"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useTranslations } from "next-intl"
import { legalPath } from "@/lib/legal"
import { SITE } from "@/lib/site"
import { LEGAL_DOCUMENTS } from "@/types/legal"

// 문서 화면 머리 줄 (specs/012-terms-privacy FR-011, US1-AC6). 인쇄 때 숨긴다
//   왼쪽 사이트 이름(첫 화면 /. 관리자 계정은 사용자 셸이 /admin으로 보낸다), 오른쪽 두 문서 링크(지금 문서는 aria-current)
export function LegalHeader() {
  const t = useTranslations("legal")
  const pathname = usePathname()
  return (
    <header className="border-b print:hidden">
      <div className="mx-auto flex h-(--header-height) w-full max-w-3xl items-center gap-4 px-4">
        <Link href="/" className="font-semibold">
          {SITE.name}
        </Link>
        <nav aria-label={t("documents")} className="ml-auto flex items-center gap-4 text-sm">
          {LEGAL_DOCUMENTS.map((document) => {
            const current = pathname === legalPath(document) || pathname.startsWith(`${legalPath(document)}/`)
            return (
              <Link
                key={document}
                href={legalPath(document)}
                aria-current={current ? "page" : undefined}
                className="text-muted-foreground underline-offset-4 hover:underline aria-[current=page]:font-semibold aria-[current=page]:text-foreground"
              >
                {t(document)}
              </Link>
            )
          })}
        </nav>
      </div>
    </header>
  )
}
