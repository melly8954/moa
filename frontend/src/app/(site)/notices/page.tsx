"use client"

import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query"
import { Pin } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useEffect, useState } from "react"
import { api } from "@/api"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { NEW_NOTICE_QUERY_KEY } from "@/components/notice/use-new-notice"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDate } from "@/lib/format"
import { cn } from "@/lib/utils"
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
            {/* 표: 번호(게시 순번, 고정 공지는 고정 표시)·제목·게시일 (FR-010). 좁은 폭에서도 넘치지 않게 열 폭을 고정한다 */}
            <Table className="table-fixed">
              <caption className="sr-only">{t("title")}</caption>
              <colgroup>
                <col className="w-16" />
                <col />
                <col className="w-28" />
              </colgroup>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("number")}</TableHead>
                  <TableHead>{t("fieldTitle")}</TableHead>
                  <TableHead>{t("publishedDate")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((n) => (
                  <TableRow key={n.id}>
                    <TableCell>
                      {n.pinned ? (
                        <span className="flex items-center gap-1 font-medium">
                          <Pin className="size-4" aria-hidden />
                          <span className="sr-only">{t("pinnedMark")}</span>
                        </span>
                      ) : (
                        <span className="text-muted-foreground tabular-nums">{n.number}</span>
                      )}
                    </TableCell>
                    <TableCell className="truncate">
                      <Link
                        href={`/notices/${n.id}`}
                        className={cn("underline-offset-4 hover:underline", n.pinned && "font-medium")}
                      >
                        {n.title}
                      </Link>
                    </TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">
                      <time dateTime={n.publishedAt}>{formatDate(n.publishedAt)}</time>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
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
