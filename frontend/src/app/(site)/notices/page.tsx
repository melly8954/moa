"use client"

import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query"
import { Pin } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useEffect, useState } from "react"
import { api } from "@/api"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { NEW_NOTICE_QUERY_KEY } from "@/components/notice/new-notice-alert"
import { Button } from "@/components/ui/button"
import { formatDate } from "@/lib/format"
import { useAuthStore } from "@/stores/auth"

const SIZE = 20

// 사용자 공지 목록 (specs/006-notices US2). 로그인 없이 연다. 고정 먼저, 그다음 게시 최신순
// 로그인 상태면 열 때 확인을 기록해 새 공지 표시를 지운다 (US3)
export default function NoticesPage() {
  const t = useTranslations("notice")
  const tt = useTranslations("table")
  const queryClient = useQueryClient()
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const [page, setPage] = useState(0)
  const notices = useQuery({
    queryKey: ["notices", page],
    queryFn: () => api.v1.notice.getPage({ page, size: SIZE }),
    placeholderData: keepPreviousData,
  })

  useEffect(() => {
    if (!signedIn) return
    api.v1.notice
      .markSeen()
      .then(() => queryClient.invalidateQueries({ queryKey: NEW_NOTICE_QUERY_KEY }))
      .catch(() => undefined)
  }, [signedIn, queryClient])

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />
      <QueryState query={notices} isEmpty={(data) => data.content.length === 0} empty={t("empty")}>
        {(data) => (
          <>
            <ul className="flex flex-col divide-y rounded-md border">
              {data.content.map((n) => (
                <li key={n.id}>
                  <Link
                    href={`/notices/${n.id}`}
                    className="flex min-w-0 items-center gap-3 px-4 py-3 hover:bg-muted focus-visible:bg-muted"
                  >
                    {n.pinned && (
                      <span className="flex shrink-0 items-center gap-1 text-sm font-medium">
                        <Pin className="size-4" aria-hidden />
                        {t("pinnedMark")}
                      </span>
                    )}
                    <span className="min-w-0 flex-1 truncate">{n.title}</span>
                    <time dateTime={n.publishedAt} className="shrink-0 text-sm text-muted-foreground">
                      {formatDate(n.publishedAt)}
                    </time>
                  </Link>
                </li>
              ))}
            </ul>
            {data.totalPages > 1 && (
              <nav aria-label={tt("pagination")} className="flex items-center justify-center gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  {tt("previous")}
                </Button>
                <span className="text-sm">
                  {page + 1} / {data.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  {tt("next")}
                </Button>
              </nav>
            )}
          </>
        )}
      </QueryState>
    </>
  )
}
