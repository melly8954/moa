"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { type Column, DataTable } from "@/components/kit/data-table"
import { LinkButton } from "@/components/kit/link-button"
import { ListPage } from "@/components/kit/page-header"
import { ChoiceFilter, KeywordFilter, SearchField, SearchPanel, SearchRow } from "@/components/kit/search-panel"
import { StatusText } from "@/components/kit/status-text"
import { useListSearch } from "@/lib/list-search"
import type { AdminNoticeListItem, NoticeStatus } from "@/types/notice"

const DEFAULTS = { keyword: "", status: "" }
const STATUSES: NoticeStatus[] = ["DRAFT", "PUBLISHED"]

// 공지 관리 (specs/006-notices, 권한 NOTICE_MANAGE). 임시 저장 포함, 고친 최신순
// 제목을 누르면 고치기 화면으로 간다
export default function AdminNoticesPage() {
  const t = useTranslations("notice")
  const search = useListSearch({ defaults: DEFAULTS })
  const notices = useQuery({
    queryKey: ["admin", "notices", search.params],
    queryFn: () => api.v1.noticeAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })

  const columns: Column<AdminNoticeListItem>[] = [
    {
      key: "title",
      header: t("fieldTitle"),
      cell: (n) => (
        <Link href={`/admin/notices/${n.id}`} className="underline-offset-4 hover:underline">
          {n.title}
        </Link>
      ),
    },
    {
      key: "status",
      header: t("status"),
      kind: "status",
      cell: (n) => (
        <StatusText tone={n.status === "PUBLISHED" ? "positive" : "neutral"}>{t(`statuses.${n.status}`)}</StatusText>
      ),
    },
    { key: "pinned", header: t("pinned"), kind: "status", cell: (n) => (n.pinned ? t("pinnedMark") : "") },
    { key: "publishedAt", header: t("publishedAt"), kind: "datetime" },
    { key: "updatedAt", header: t("updatedAt"), kind: "datetime" },
  ]

  return (
    <ListPage
      title={t("adminTitle")}
      description={t("adminDescription")}
      actions={
        <LinkButton href="/admin/notices/new">
          <Plus />
          {t("create")}
        </LinkButton>
      }
      search={
        <SearchPanel search={search}>
          <SearchRow>
            <SearchField label={t("status")}>
              <ChoiceFilter name="status" options={STATUSES.map((s) => ({ value: s, label: t(`statuses.${s}`) }))} />
            </SearchField>
          </SearchRow>
          <SearchField label={t("keyword")}>
            <KeywordFilter name="keyword" placeholder={t("keywordPlaceholder")} />
          </SearchField>
        </SearchPanel>
      }
    >
      <DataTable caption={t("adminTitle")} columns={columns} query={notices} search={search} rowKey={(n) => n.id} />
    </ListPage>
  )
}
