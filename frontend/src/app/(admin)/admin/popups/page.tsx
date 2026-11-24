"use client"

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import { KeywordFilter, SearchField, SearchPanel } from "@/components/kit/search-panel"
import { ExposureStatusText } from "@/components/marketing/exposure-status"
import { ExposureStatusFilter } from "@/components/marketing/exposure-status-filter"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import { useListSearch } from "@/lib/list-search"
import { periodText } from "@/lib/marketing"
import type { AdminPopupListItem } from "@/types/popup"
import { PopupDrawer } from "./popup-drawer"

// 팝업 관리 (specs/016-banner-popup US2, 권한 POPUP_MANAGE)
//   배너 관리와 같은 목록 + 드로어 + 체크박스 일괄 처리. 제목으로 찾는다 (FR-008)
const DEFAULTS = { keyword: "", status: "" }

export default function PopupsPage() {
  const t = useTranslations("popup")
  const tm = useTranslations("marketing")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const search = useListSearch({ defaults: DEFAULTS })
  const popups = useQuery({
    queryKey: ["admin", "popups", search.params],
    queryFn: () => api.v1.popupAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })
  const [creating, setCreating] = useState(false)
  const [openId, setOpenId] = useState<number>()
  const opened = useQuery({
    queryKey: ["admin", "popups", "detail", openId],
    queryFn: () => api.v1.popupAdmin.get(openId as number),
    enabled: openId !== undefined,
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "popups"] })

  const rows = popups.data?.content ?? []
  const paramsKey = JSON.stringify(search.params)
  const [picked, setPicked] = useState<{ key: string; ids: number[] }>({ key: paramsKey, ids: [] })
  const selected = picked.key === paramsKey ? picked.ids.filter((id) => rows.some((p) => p.id === id)) : []

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(describeError(toApiError(error)).message),
  })

  const bulkEnable = (enabled: boolean) => {
    const ids = selected
    run.mutate(async () => {
      await api.v1.popupAdmin.setEnabled(ids, enabled)
      setPicked({ key: paramsKey, ids: [] })
      toast.success(t(enabled ? "bulkEnabled" : "bulkDisabled", { count: ids.length }))
    })
  }

  const bulkRemove = async () => {
    const ids = selected
    const ok = await confirm({
      title: t("bulkRemoveTitle", { count: ids.length }),
      description: t("bulkRemoveDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(async () => {
      await api.v1.popupAdmin.bulkDelete(ids)
      setPicked({ key: paramsKey, ids: [] })
      if (openId !== undefined && ids.includes(openId)) setOpenId(undefined)
      toast.success(t("bulkRemoved", { count: ids.length }))
    })
  }

  const remove = async () => {
    const popup = opened.data
    if (!popup) return
    const ok = await confirm({
      title: t("removeTitle", { title: popup.title }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(async () => {
      await api.v1.popupAdmin.remove(popup.id)
      setOpenId(undefined)
      toast.success(t("removed"))
    })
  }

  const columns: Column<AdminPopupListItem>[] = [
    {
      key: "title",
      header: t("fieldTitle"),
      cell: (p) => (
        <button type="button" className="text-left" onClick={() => setOpenId(p.id)}>
          {p.title}
        </button>
      ),
    },
    { key: "period", header: tm("period"), cell: (p) => periodText(p.startsAt, p.endsAt) },
    { key: "sortOrder", header: tm("sortOrder"), kind: "number" },
    { key: "status", header: tm("status"), kind: "status", cell: (p) => <ExposureStatusText status={p.status} /> },
  ]

  const nothing = selected.length === 0 || run.isPending
  return (
    <ListPage
      title={t("title")}
      description={t("description")}
      actions={
        <Button onClick={() => setCreating(true)}>
          <Plus />
          {t("create")}
        </Button>
      }
      search={
        <SearchPanel search={search}>
          <ExposureStatusFilter search={search} />
          <SearchField label={t("fieldTitle")}>
            <KeywordFilter name="keyword" placeholder={t("keywordPlaceholder")} />
          </SearchField>
        </SearchPanel>
      }
    >
      <div className="flex flex-col gap-2">
        <BulkToolbar
          label={t("bulkToolbar")}
          selected={selected.length > 0 ? t("bulkSelected", { count: selected.length }) : t("bulkHint")}
        >
          <Button size="sm" variant="outline" disabled={nothing} onClick={() => bulkEnable(true)}>
            {t("enable")}
          </Button>
          <Button size="sm" variant="outline" disabled={nothing} onClick={() => bulkEnable(false)}>
            {t("disable")}
          </Button>
          <Button size="sm" variant="outline" disabled={nothing} onClick={() => void bulkRemove()}>
            {t("remove")}
          </Button>
        </BulkToolbar>
        <DataTable
          caption={t("title")}
          columns={columns}
          query={popups}
          search={search}
          rowKey={(p) => p.id}
          onRowClick={(p) => setOpenId(p.id)}
          selection={{
            selected,
            onChange: (ids) => setPicked({ key: paramsKey, ids: ids.map(Number) }),
            rowLabel: (p) => p.title,
          }}
        />
      </div>
      {creating && <PopupDrawer open onOpenChange={setCreating} onSaved={invalidate} />}
      {opened.data && openId === opened.data.id && (
        <PopupDrawer
          key={`${opened.data.id}-${opened.data.updatedAt}`}
          popup={opened.data}
          open
          onOpenChange={(open) => !open && setOpenId(undefined)}
          onSaved={invalidate}
          onRemove={() => void remove()}
        />
      )}
    </ListPage>
  )
}
