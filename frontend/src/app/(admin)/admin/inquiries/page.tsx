"use client"

import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import {
  KeywordFilter,
  PeriodFilter,
  SearchField,
  SearchPanel,
  SearchRow,
  SelectFilter,
} from "@/components/kit/search-panel"
import { InquiryStatusText } from "@/components/support/inquiry-status"
import { InquiryStatusFilter } from "@/components/support/inquiry-status-filter"
import { useSupportCategories } from "@/components/support/use-support-categories"
import { formatDateTime } from "@/lib/format"
import { useListSearch } from "@/lib/list-search"
import { api } from "@/api"
import type { AdminInquiryListItem } from "@/types/inquiry"
import { authorText } from "./author"
import { InquiryDrawer } from "./inquiry-drawer"

// 문의 관리 (specs/017-customer-center US4, 권한 INQUIRY_MANAGE)
//   업무 목록 (FR-015): 기본 거르기 "답변 대기", 분류, 쓴 날짜 기간, 검색어(제목·작성자). 쓴 최신순
//   행을 누르면 드로어에서 읽고 답한다. 문의를 지우거나 한꺼번에 처리하는 기능은 없다 (FR-010)
const DEFAULTS = { status: "", categoryCode: "", from: "", to: "", keyword: "" }

export default function InquiriesPage() {
  const t = useTranslations("inquiry")
  const queryClient = useQueryClient()
  const search = useListSearch({ defaults: DEFAULTS })
  const categories = useSupportCategories()
  const inquiries = useQuery({
    queryKey: ["admin", "inquiries", search.params],
    queryFn: () => api.v1.inquiryAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })
  const [openId, setOpenId] = useState<number>()
  const opened = useQuery({
    queryKey: ["admin", "inquiries", "detail", openId],
    queryFn: () => api.v1.inquiryAdmin.get(openId as number),
    enabled: openId !== undefined,
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "inquiries"] })

  const columns: Column<AdminInquiryListItem>[] = [
    { key: "status", header: t("status"), kind: "status", cell: (i) => <InquiryStatusText status={i.status} /> },
    { key: "category", header: t("fieldCategory"), cell: (i) => i.categoryName },
    {
      key: "title",
      header: t("fieldTitle"),
      cell: (i) => (
        <button type="button" className="text-left" onClick={() => setOpenId(i.id)}>
          {i.title}
        </button>
      ),
    },
    { key: "author", header: t("author"), cell: (i) => authorText(i.author, t("authorWithdrawn")) },
    { key: "createdAt", header: t("createdAt"), kind: "datetime", cell: (i) => formatDateTime(i.createdAt) },
    {
      key: "answeredAt",
      header: t("answeredAt"),
      kind: "datetime",
      cell: (i) => (i.answeredAt ? formatDateTime(i.answeredAt) : "-"),
    },
  ]

  const categoryOptions = (categories.data ?? []).map((c) => ({ value: c.code, label: c.name }))
  return (
    <ListPage
      title={t("title")}
      description={t.rich("description", { link: (chunks) => <Link href="/admin/faqs">{chunks}</Link> })}
      search={
        <SearchPanel search={search}>
          <InquiryStatusFilter search={search} />
          <SearchRow>
            <SearchField label={t("fieldCategory")}>
              <SelectFilter name="categoryCode" options={categoryOptions} />
            </SearchField>
            <SearchField label={t("createdAt")}>
              <PeriodFilter from="from" to="to" />
            </SearchField>
          </SearchRow>
          <SearchField label={t("keyword")}>
            <KeywordFilter name="keyword" placeholder={t("keywordPlaceholder")} />
          </SearchField>
        </SearchPanel>
      }
    >
      <DataTable
        caption={t("title")}
        columns={columns}
        query={inquiries}
        search={search}
        rowKey={(i) => i.id}
        onRowClick={(i) => setOpenId(i.id)}
      />
      {opened.data && openId === opened.data.id && (
        <InquiryDrawer
          key={`${opened.data.id}-${opened.data.answeredAt ?? ""}-${opened.data.answer ?? ""}`}
          inquiry={opened.data}
          open
          onOpenChange={(open) => !open && setOpenId(undefined)}
          onSaved={invalidate}
          onMissing={() => {
            setOpenId(undefined)
            void invalidate()
          }}
        />
      )}
    </ListPage>
  )
}
