"use client"

import { useMemo, useSyncExternalStore } from "react"
import { sanitizeRichText } from "@/lib/rich-text"
import { cn } from "@/lib/utils"
import { RICH_TEXT_CONTENT_CLASS } from "./content-class"

const subscribe = () => () => {}

// 저장된 서식 있는 글(HTML)을 그린다 (공지 상세 등)
//   <RichTextView html={notice.content} />
// 서버가 저장할 때 걸렀지만, 그리기 전에 같은 허용 목록(lib/rich-text.ts)으로 한 번 더 거른다
// 거르기에 DOM이 필요해 브라우저에서만 그린다 (서버 렌더에서는 빈 칸. 본문은 어차피 브라우저에서 API로 받는다)
export function RichTextView({ html, className }: { html: string | null | undefined; className?: string }) {
  const inBrowser = useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  )
  const safe = useMemo(() => (inBrowser ? sanitizeRichText(html) : ""), [inBrowser, html])
  return (
    <div
      data-slot="rich-text"
      className={cn(RICH_TEXT_CONTENT_CLASS, "min-w-0", className)}
      // 위에서 허용 목록으로 걸렀다 (sanitizeRichText)
      dangerouslySetInnerHTML={{ __html: safe }}
    />
  )
}
