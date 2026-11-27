"use client"

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
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
import type { AdminBannerListItem } from "@/types/banner"
import { BannerDrawer } from "./banner-drawer"

// 배너 관리 (specs/016-banner-popup US1, 권한 BANNER_MANAGE)
//   목록 + 드로어 + 체크박스 일괄 처리 (FR-008, 001-admin-menus "관리 화면 정리")
//   상태 거르기 기본은 "끝남 빼고"(빈 값), 전체는 ALL (research R4). 이름으로 찾는다
//   일괄 처리 줄: 켜기·끄기·삭제. 삭제만 확인을 받는다. 하나라도 없으면 서버가 아무것도 바꾸지 않는다(R001)
//   고른 것은 이 조건(쪽·검색) 안에서만 산다. 조건이 바뀌거나 처리가 끝나면 풀린다 (research R7)
const DEFAULTS = { keyword: "", status: "" }

export default function BannersPage() {
  const t = useTranslations("banner")
  const tm = useTranslations("marketing")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const search = useListSearch({ defaults: DEFAULTS })
  const banners = useQuery({
    queryKey: ["admin", "banners", search.params],
    queryFn: () => api.v1.bannerAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })
  const [creating, setCreating] = useState(false)
  const [openId, setOpenId] = useState<number>()
  const opened = useQuery({
    queryKey: ["admin", "banners", "detail", openId],
    queryFn: () => api.v1.bannerAdmin.get(openId as number),
    enabled: openId !== undefined,
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "banners"] })

  const rows = banners.data?.content ?? []
  const paramsKey = JSON.stringify(search.params)
  const [picked, setPicked] = useState<{ key: string; ids: number[] }>({ key: paramsKey, ids: [] })
  const selected = picked.key === paramsKey ? picked.ids.filter((id) => rows.some((b) => b.id === id)) : []

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(describeError(toApiError(error)).message),
  })

  const bulkEnable = (enabled: boolean) => {
    const ids = selected
    run.mutate(async () => {
      await api.v1.bannerAdmin.setEnabled(ids, enabled)
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
      await api.v1.bannerAdmin.bulkDelete(ids)
      setPicked({ key: paramsKey, ids: [] })
      if (openId !== undefined && ids.includes(openId)) setOpenId(undefined)
      toast.success(t("bulkRemoved", { count: ids.length }))
    })
  }

  const remove = async () => {
    const banner = opened.data
    if (!banner) return
    const ok = await confirm({
      title: t("removeTitle", { name: banner.name }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(async () => {
      await api.v1.bannerAdmin.remove(banner.id)
      setOpenId(undefined)
      toast.success(t("removed"))
    })
  }

  const columns: Column<AdminBannerListItem>[] = [
    {
      key: "image",
      header: t("image"),
      className: "w-28",
      cell: (b) => (
        // 목록의 작은 미리보기 (원본을 그대로 줄여 보인다)
        // eslint-disable-next-line @next/next/no-img-element
        <img src={b.wideImageUrl} alt="" className="aspect-[4/1] w-24 rounded-sm border object-cover" />
      ),
    },
    {
      key: "name",
      header: t("name"),
      cell: (b) => (
        <button type="button" className="text-left" onClick={() => setOpenId(b.id)}>
          {b.name}
        </button>
      ),
    },
    { key: "period", header: tm("period"), cell: (b) => periodText(b.startsAt, b.endsAt) },
    { key: "sortOrder", header: tm("sortOrder"), kind: "number" },
    { key: "status", header: tm("status"), kind: "status", cell: (b) => <ExposureStatusText status={b.status} /> },
  ]

  const nothing = selected.length === 0 || run.isPending
  return (
    <ListPage
      title={t("title")}
      description={t.rich("description", { link: (chunks) => <Link href="/admin/notices">{chunks}</Link> })}
      actions={
        <Button onClick={() => setCreating(true)}>
          <Plus />
          {t("create")}
        </Button>
      }
      search={
        <SearchPanel search={search}>
          <ExposureStatusFilter search={search} />
          <SearchField label={t("name")}>
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
          query={banners}
          search={search}
          rowKey={(b) => b.id}
          onRowClick={(b) => setOpenId(b.id)}
          selection={{
            selected,
            onChange: (ids) => setPicked({ key: paramsKey, ids: ids.map(Number) }),
            rowLabel: (b) => b.name,
          }}
        />
      </div>
      {creating && <BannerDrawer open onOpenChange={setCreating} onSaved={invalidate} />}
      {opened.data && openId === opened.data.id && (
        <BannerDrawer
          key={`${opened.data.id}-${opened.data.updatedAt}`}
          banner={opened.data}
          open
          onOpenChange={(open) => !open && setOpenId(undefined)}
          onSaved={invalidate}
          onRemove={() => void remove()}
        />
      )}
    </ListPage>
  )
}
