"use client"

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { type Column, DataTable } from "@/components/kit/data-table"
import { LinkButton } from "@/components/kit/link-button"
import { ListPage } from "@/components/kit/page-header"
import { ChoiceFilter, KeywordFilter, SearchField, SearchPanel, SearchRow } from "@/components/kit/search-panel"
import { StatusText } from "@/components/kit/status-text"
import { useNoticeErrorText } from "@/components/notice/use-notice-error"
import { NEW_NOTICE_QUERY_KEY } from "@/components/notice/use-new-notice"
import { Button } from "@/components/ui/button"
import { useListSearch } from "@/lib/list-search"
import type { AdminNoticeListItem, NoticeStatus } from "@/types/notice"

const DEFAULTS = { keyword: "", status: "" }
const STATUSES: NoticeStatus[] = ["DRAFT", "PUBLISHED"]

// 공지 관리 (specs/006-notices, 권한 NOTICE_MANAGE). 임시 저장 포함, 고친 최신순
// 제목을 누르면 고치기 화면으로 간다
// 체크해서 한꺼번에 지운다 (2026-11-24 FR-001, research R13). 일괄 처리는 삭제만이고, 모든 행을 고를 수 있다
//   고른 것은 이 조건(쪽·검색) 안에서만 산다. 조건이 바뀌거나 지워져 목록에서 빠진 행은 고른 것에서 빠진다
export default function AdminNoticesPage() {
  const t = useTranslations("notice")
  const queryClient = useQueryClient()
  const errorText = useNoticeErrorText()
  const search = useListSearch({ defaults: DEFAULTS })
  const notices = useQuery({
    queryKey: ["admin", "notices", search.params],
    queryFn: () => api.v1.noticeAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })
  const rows = notices.data?.content ?? []
  const paramsKey = JSON.stringify(search.params)
  const [picked, setPicked] = useState<{ key: string; ids: number[] }>({ key: paramsKey, ids: [] })
  const selected = picked.key === paramsKey ? picked.ids.filter((id) => rows.some((n) => n.id === id)) : []

  const remove = useMutation({
    mutationFn: (ids: number[]) => api.v1.noticeAdmin.bulkDelete(ids),
    onSuccess: async (_, ids) => {
      setPicked({ key: paramsKey, ids: [] })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin", "notices"] }),
        queryClient.invalidateQueries({ queryKey: NEW_NOTICE_QUERY_KEY }),
      ])
      toast.success(t("bulkRemoved", { count: ids.length }))
    },
    onError: (error) => toast.error(errorText(error)),
  })

  const askRemove = async () => {
    const ids = selected
    const ok = await confirm({
      title: t("bulkRemoveTitle", { count: ids.length }),
      description: t("bulkRemoveDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate(ids)
  }

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
      description={t.rich("adminDescription", { link: (chunks) => <Link href="/notices">{chunks}</Link> })}
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
      <div className="flex flex-col gap-2">
        {/* 일괄 처리 줄은 늘 같은 자리·높이 (001-admin-menus FR-010) */}
        <BulkToolbar
          label={t("bulkToolbar")}
          selected={selected.length > 0 ? t("bulkSelected", { count: selected.length }) : t("bulkHint")}
        >
          <Button
            size="sm"
            variant="outline"
            disabled={selected.length === 0 || remove.isPending}
            onClick={() => void askRemove()}
          >
            {t("remove")}
          </Button>
        </BulkToolbar>
        <DataTable
          caption={t("adminTitle")}
          columns={columns}
          query={notices}
          search={search}
          rowKey={(n) => n.id}
          selection={{
            selected,
            onChange: (ids) => setPicked({ key: paramsKey, ids: ids.map(Number) }),
            rowLabel: (n) => n.title,
          }}
        />
      </div>
    </ListPage>
  )
}
