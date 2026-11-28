"use client"

import { useTranslations } from "next-intl"
import { useMemo, useSyncExternalStore } from "react"
import { sanitizeLegalHtml } from "@/lib/legal"
import { cn } from "@/lib/utils"
import type { LegalSection, LegalVersionContent } from "@/types/legal"

const subscribe = () => () => {}

// 장 본문 모양. 표는 표 안에서만 가로로 넘긴다 (US1-AC4, research R3)
const SECTION_CLASS = [
  "text-sm leading-relaxed break-words",
  "[&_p]:my-2 [&_h3]:mt-4 [&_h3]:mb-2 [&_h3]:text-base [&_h3]:font-semibold",
  "[&_ol]:my-2 [&_ol]:list-decimal [&_ol]:pl-6 [&_ul]:my-2 [&_ul]:list-disc [&_ul]:pl-6",
  "[&_a]:font-medium [&_a]:underline [&_a]:underline-offset-4",
  "[&_table]:my-3 [&_table]:block [&_table]:max-w-full [&_table]:overflow-x-auto [&_table]:border-collapse",
  "[&_caption]:mb-1 [&_caption]:text-left [&_caption]:text-xs [&_caption]:text-muted-foreground",
  "[&_td]:border [&_td]:px-2 [&_td]:py-1.5 [&_td]:align-top [&_th]:border [&_th]:bg-muted [&_th]:px-2 [&_th]:py-1.5 [&_th]:text-left [&_th]:font-semibold",
  "[&_td]:min-w-28 [&_th]:min-w-28",
  "[&>:first-child]:mt-0 [&>:last-child]:mb-0",
].join(" ")

// 문서 버전 하나를 그린다 (specs/012-terms-privacy contracts/screens.md "화면 구성" 3·5·6)
//   제목, "시행일 · 버전" 줄, 제목 아래에 끼울 것(버전 목록), 목차, 장
//   compact: 가입 화면의 약관 칸. 제목·목차 없이 높이가 정해진 상자에서 스크롤한다 (FR-005)
// 본문 거르기에 DOM이 필요해 브라우저에서만 그린다 (서버 렌더에서는 빈 칸. 본문은 어차피 브라우저에서 API로 받는다)
export function LegalDocument({
  content,
  afterMeta,
  compact,
  label,
}: {
  content: LegalVersionContent
  afterMeta?: React.ReactNode
  compact?: boolean
  // compact 상자의 이름 (화면 읽기 프로그램)
  label?: string
}) {
  const t = useTranslations("legal")
  if (compact) {
    return (
      <div
        role="region"
        aria-label={label ?? content.title}
        tabIndex={0}
        className="max-h-48 overflow-y-auto rounded-md border bg-muted/30 p-3"
      >
        <div className="flex flex-col gap-3">
          {content.sections.map((section) => (
            <Section key={section.id} section={section} compact />
          ))}
        </div>
      </div>
    )
  }
  return (
    <article className="flex min-w-0 flex-col gap-6">
      <header className="flex flex-col gap-2 border-b pb-4">
        <h1 className="text-2xl font-semibold break-words">{content.title}</h1>
        <p className="text-sm text-muted-foreground">
          {t("meta", { date: content.effectiveDate, version: content.version })}
        </p>
        {afterMeta}
      </header>
      <nav aria-label={t("toc")} className="rounded-md border p-4 print:hidden">
        <h2 className="mb-2 text-sm font-semibold">{t("toc")}</h2>
        <ol className="flex list-decimal flex-col gap-1 pl-5 text-sm">
          {content.sections.map((section) => (
            <li key={section.id}>
              <a href={`#${section.id}`} className="underline-offset-4 hover:underline">
                {section.heading}
              </a>
            </li>
          ))}
        </ol>
      </nav>
      {content.sections.map((section) => (
        <Section key={section.id} section={section} />
      ))}
    </article>
  )
}

function Section({ section, compact }: { section: LegalSection; compact?: boolean }) {
  const inBrowser = useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  )
  const safe = useMemo(() => (inBrowser ? sanitizeLegalHtml(section.html) : ""), [inBrowser, section.html])
  // compact는 상자 안이라 장 바로 가기 id를 두지 않는다 (같은 화면에 문서 화면이 없다)
  const Heading = compact ? "h3" : "h2"
  return (
    <section id={compact ? undefined : section.id} className="flex min-w-0 scroll-mt-4 flex-col gap-2">
      <Heading className={cn("font-semibold", compact ? "text-sm" : "text-lg")}>{section.heading}</Heading>
      <div
        className={SECTION_CLASS}
        // 위에서 허용 목록으로 걸렀다 (sanitizeLegalHtml)
        dangerouslySetInnerHTML={{ __html: safe }}
      />
    </section>
  )
}
