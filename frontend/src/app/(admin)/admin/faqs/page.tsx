"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { ListPage } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { openOnRowClick, ROW_CLICKABLE } from "@/components/kit/row-click"
import {
  ChoiceFilter,
  KeywordFilter,
  SearchField,
  SearchPanel,
  SearchRow,
  SelectFilter,
} from "@/components/kit/search-panel"
import { StatusText } from "@/components/kit/status-text"
import { useSupportCategories } from "@/components/support/use-support-categories"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { useListSearch } from "@/lib/list-search"
import type { AdminFaqListItem } from "@/types/faq"
import { FaqDrawer } from "./faq-drawer"

// FAQ 관리 (specs/017-customer-center US1, 권한 FAQ_MANAGE)
//   구성 목록이라 쪽 없이 전체를 표 안에서 스크롤한다 (FR-015, 상한 200). 분류·켜짐·질문으로 거른다
//   체크박스로 골라 한꺼번에 켜고 끄고 지운다 (FR-005). 고른 것은 검색이 바뀌거나 처리에 성공하면 풀린다
const DEFAULTS = { categoryCode: "", enabled: "", keyword: "" }

export default function FaqsPage() {
  const t = useTranslations("faq")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const search = useListSearch({ defaults: DEFAULTS })
  const categories = useSupportCategories()
  // 빈 조건은 보내지 않는다
  const filters = Object.fromEntries(Object.entries(search.filters).filter(([, v]) => v !== ""))
  const faqs = useQuery({
    queryKey: ["admin", "faqs", filters],
    queryFn: () => api.v1.faqAdmin.list(filters),
  })
  const [creating, setCreating] = useState(false)
  const [openId, setOpenId] = useState<number>()
  const opened = useQuery({
    queryKey: ["admin", "faqs", "detail", openId],
    queryFn: () => api.v1.faqAdmin.get(openId as number),
    enabled: openId !== undefined,
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "faqs"] })

  const rows = faqs.data ?? []
  const filtersKey = JSON.stringify(filters)
  const [picked, setPicked] = useState<{ key: string; ids: number[] }>({ key: filtersKey, ids: [] })
  const selected = picked.key === filtersKey ? picked.ids.filter((id) => rows.some((f) => f.id === id)) : []
  const setSelected = (ids: number[]) => setPicked({ key: filtersKey, ids })
  const allChecked = rows.length > 0 && rows.every((f) => selected.includes(f.id))
  const someChecked = !allChecked && selected.length > 0

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(describeError(toApiError(error)).message),
  })

  const bulkEnable = (enabled: boolean) => {
    const ids = selected
    run.mutate(async () => {
      await api.v1.faqAdmin.setEnabled(ids, enabled)
      setSelected([])
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
      await api.v1.faqAdmin.bulkDelete(ids)
      setSelected([])
      if (openId !== undefined && ids.includes(openId)) setOpenId(undefined)
      toast.success(t("bulkRemoved", { count: ids.length }))
    })
  }

  const remove = async () => {
    const faq = opened.data
    if (!faq) return
    const ok = await confirm({
      title: t("removeTitle", { question: faq.question }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(async () => {
      await api.v1.faqAdmin.remove(faq.id)
      setOpenId(undefined)
      toast.success(t("removed"))
    })
  }

  const categoryOptions = (categories.data ?? []).map((c) => ({ value: c.code, label: c.name }))
  const nothing = selected.length === 0 || run.isPending
  return (
    <ListPage
      title={t("title")}
      description={t.rich("description", { link: (chunks) => <Link href="/admin/inquiries">{chunks}</Link> })}
      actions={
        <Button onClick={() => setCreating(true)}>
          <Plus />
          {t("create")}
        </Button>
      }
      search={
        <SearchPanel search={search}>
          <SearchRow>
            <SearchField label={t("fieldCategory")}>
              <SelectFilter name="categoryCode" options={categoryOptions} />
            </SearchField>
            <SearchField label={t("enabled")}>
              <ChoiceFilter
                name="enabled"
                options={[
                  { value: "true", label: t("enabledOn") },
                  { value: "false", label: t("enabledOff") },
                ]}
              />
            </SearchField>
          </SearchRow>
          <SearchField label={t("fieldQuestion")}>
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
        <QueryState query={faqs} isEmpty={(data) => data.length === 0} empty={t("empty")}>
          {(data) => (
            // 구성 목록: 표 안에서 스크롤한다 (FR-015)
            <div className="max-h-[60vh] overflow-auto rounded-md border">
              <Table className="table-fixed">
                <caption className="sr-only">{t("title")}</caption>
                <TableHeader className="sticky top-0 z-10 bg-background">
                  <TableRow>
                    <TableHead className="w-10">
                      <Checkbox
                        aria-label={t("selectAll")}
                        checked={allChecked}
                        indeterminate={someChecked}
                        onCheckedChange={(c) => setSelected(c === true ? data.map((f) => f.id) : [])}
                      />
                    </TableHead>
                    <TableHead className="w-24 sm:w-32">{t("fieldCategory")}</TableHead>
                    <TableHead>{t("fieldQuestion")}</TableHead>
                    <TableHead className="w-16 text-right">{t("sortOrder")}</TableHead>
                    <TableHead className="w-24">{t("enabled")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.map((f: AdminFaqListItem) => (
                    <TableRow
                      key={f.id}
                      data-state={selected.includes(f.id) ? "selected" : undefined}
                      className={ROW_CLICKABLE}
                      onClick={openOnRowClick(() => setOpenId(f.id))}
                    >
                      <TableCell>
                        <Checkbox
                          aria-label={t("selectOf", { question: f.question })}
                          checked={selected.includes(f.id)}
                          onCheckedChange={(c) =>
                            setSelected(c === true ? [...selected, f.id] : selected.filter((id) => id !== f.id))
                          }
                        />
                      </TableCell>
                      <TableCell className="truncate">{f.categoryName}</TableCell>
                      <TableCell className="truncate">
                        <button type="button" className="max-w-full truncate text-left" onClick={() => setOpenId(f.id)}>
                          {f.question}
                        </button>
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{f.sortOrder}</TableCell>
                      <TableCell>
                        <StatusText tone={f.enabled ? "positive" : "neutral"}>
                          {f.enabled ? t("enabledOn") : t("enabledOff")}
                        </StatusText>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </QueryState>
      </div>
      {creating && <FaqDrawer open onOpenChange={setCreating} onSaved={invalidate} />}
      {opened.data && openId === opened.data.id && (
        <FaqDrawer
          key={`${opened.data.id}-${opened.data.updatedAt}`}
          faq={opened.data}
          open
          onOpenChange={(open) => !open && setOpenId(undefined)}
          onSaved={invalidate}
          onRemove={() => void remove()}
        />
      )}
    </ListPage>
  )
}
