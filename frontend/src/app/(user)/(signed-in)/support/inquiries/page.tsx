"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { api } from "@/api"
import { LinkButton } from "@/components/kit/link-button"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { InquiryStatusText } from "@/components/support/inquiry-status"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDate } from "@/lib/format"

const SIZE = 20

// 내 문의 (specs/017-customer-center US3, FR-013). 로그인 필요. 쓴 최신순
//   업무 목록이지만 회원 한 사람의 문의라 공지 목록과 같은 이전·다음 쪽 이동을 쓴다
export default function MyInquiriesPage() {
  const t = useTranslations("inquiry")
  const tt = useTranslations("table")
  const [page, setPage] = useState(0)
  const inquiries = useQuery({
    queryKey: ["inquiries", "me", page],
    queryFn: () => api.v1.inquiry.myPage({ page, size: SIZE }),
    placeholderData: keepPreviousData,
  })
  const askButton = <LinkButton href="/support/inquiries/new">{t("ask")}</LinkButton>

  return (
    <>
      <PageHeader title={t("myTitle")} description={t("myDescription")} actions={askButton} />
      <QueryState
        query={inquiries}
        isEmpty={(data) => data.content.length === 0}
        empty={
          <div className="flex flex-col items-center gap-3 py-6 text-center">
            <p className="text-sm text-muted-foreground">{t("myEmpty")}</p>
            {askButton}
          </div>
        }
      >
        {(data) => (
          <>
            <Table className="table-fixed">
              <caption className="sr-only">{t("myTitle")}</caption>
              <colgroup>
                <col className="hidden w-24 sm:table-column" />
                <col />
                <col className="w-24" />
                <col className="w-28" />
              </colgroup>
              <TableHeader>
                <TableRow>
                  <TableHead className="hidden sm:table-cell">{t("fieldCategory")}</TableHead>
                  <TableHead>{t("fieldTitle")}</TableHead>
                  <TableHead>{t("status")}</TableHead>
                  <TableHead>{t("createdDate")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((i) => (
                  <TableRow key={i.id}>
                    <TableCell className="hidden truncate sm:table-cell">{i.categoryName}</TableCell>
                    <TableCell className="truncate">
                      <Link href={`/support/inquiries/${i.id}`} className="underline-offset-4 hover:underline">
                        {i.title}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <InquiryStatusText status={i.status} />
                    </TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">
                      <time dateTime={i.createdAt}>{formatDate(i.createdAt)}</time>
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
