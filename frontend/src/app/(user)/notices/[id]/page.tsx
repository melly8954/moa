"use client"

import { useQuery } from "@tanstack/react-query"
import { Pin } from "lucide-react"
import { useParams } from "next/navigation"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { LinkButton } from "@/components/kit/link-button"
import { QueryState } from "@/components/kit/query-state"
import { RichTextView } from "@/components/rich-text/rich-text-view"
import { formatDate } from "@/lib/format"

// 사용자 공지 상세 (specs/006-notices US2). 로그인 없이 연다
// 임시 저장·지운 공지·없는 공지(R001)는 "없는 공지" 안내와 목록 링크
export default function NoticePage() {
  const t = useTranslations("notice")
  const id = Number(useParams<{ id: string }>().id)
  const notice = useQuery({
    queryKey: ["notices", "detail", id],
    queryFn: () => api.v1.notice.get(id),
    retry: (count, error) => toApiError(error).code !== "R001" && count < 2,
  })
  const missing = notice.isError && toApiError(notice.error).code === "R001"

  return (
    <article className="flex min-w-0 flex-col gap-4">
      {missing ? (
        <div className="flex flex-col items-start gap-3">
          <h1 className="text-xl font-semibold">{t("notFound")}</h1>
          <LinkButton href="/notices" variant="outline">
            {t("backToList")}
          </LinkButton>
        </div>
      ) : (
        <QueryState query={notice}>
          {(data) => (
            <>
              <header className="flex flex-col gap-1 border-b pb-3">
                <h1 className="text-xl font-semibold break-words">{data.title}</h1>
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  {data.pinned && (
                    <span className="flex items-center gap-1 font-medium text-foreground">
                      <Pin className="size-4" aria-hidden />
                      {t("pinnedMark")}
                    </span>
                  )}
                  <span>
                    {t("publishedDate")} <time dateTime={data.publishedAt}>{formatDate(data.publishedAt)}</time>
                  </span>
                </p>
              </header>
              <RichTextView html={data.content} />
              <div>
                <LinkButton href="/notices" variant="outline">
                  {t("backToList")}
                </LinkButton>
              </div>
            </>
          )}
        </QueryState>
      )}
    </article>
  )
}
