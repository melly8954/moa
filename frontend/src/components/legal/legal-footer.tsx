"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { cn } from "@/lib/utils"

// 푸터 줄 "이용약관 · 개인정보처리방침" (specs/012-terms-privacy FR-004, contracts/screens.md "링크")
//   개인정보처리방침은 굵게 (US2-AC3). 인쇄 때 숨긴다
//   newTab: 가입 화면. 쓰던 내용을 잃지 않게 새 탭에서 연다 (US2-AC4)
export function LegalFooter({ newTab, className }: { newTab?: boolean; className?: string }) {
  const t = useTranslations("legal")
  const target = newTab ? { target: "_blank", rel: "noopener" } : {}
  return (
    <footer className={cn("py-4 text-center text-xs text-muted-foreground print:hidden", className)}>
      <nav aria-label={t("footer")} className="inline-flex items-center gap-2">
        <Link href="/terms" className="underline-offset-4 hover:underline" {...target}>
          {t("terms")}
        </Link>
        <span aria-hidden>·</span>
        <Link href="/privacy" className="font-semibold underline-offset-4 hover:underline" {...target}>
          {t("privacy")}
        </Link>
      </nav>
    </footer>
  )
}
