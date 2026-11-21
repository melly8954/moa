"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Lock, Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { PanelGuardContext, useIsWide, usePanelGuard } from "@/components/kit/detail-panel"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { openOnRowClick, ROW_CLICKABLE } from "@/components/kit/row-click"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { formatNumber } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { Role } from "@/types/role"
import { RolePanel } from "./role-panel"

// 권한 관리 (rbac 헬퍼, 권한 ROLE_MANAGE). 역할에 쓸 메뉴를 묶고, 구성원에게 역할을 준다
// moa: 두 칸 화면 (specs/003-admin-roles FR-005·FR-011, research R19, 2026-11-21)
//   넓은 폭: 왼쪽 검색·일괄 줄·역할 목록(체크박스·이름·구성원 수), 오른쪽 상세 칸. 좁은 폭: 목록 한 칸, 상세는 드로어
//   구성 목록이라 쪽 나눔 없이 전체를 보이고 이름·코드 검색은 화면에서 거른다
//   최고 관리자 역할은 맨 위에 자물쇠와 함께 두고 체크할 수 없다 (지울 수 없다, FR-010)
//   저장하지 않은 변경이 있으면 다른 역할·역할 추가로 가기 전에 묻는다
export default function RolesPage() {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const [filter, setFilter] = useState("")
  const [selected, setSelected] = useState<number[]>([])
  // 오른쪽 상세 칸: 고친다(roleId) 또는 새로 만든다(roleId 없음)
  const [panel, setPanel] = useState<{ roleId?: number } | null>(null)
  const { guard, confirmLeave } = usePanelGuard()
  const wide = useIsWide()
  const keyword = filter.trim().toLowerCase()
  const shown = (roles.data ?? []).filter(
    (r) => !keyword || r.name.toLowerCase().includes(keyword) || r.code.toLowerCase().includes(keyword),
  )
  const selectableIds = shown.filter((r) => !r.fixed).map((r) => r.id)
  const allChecked = selectableIds.length > 0 && selectableIds.every((id) => selected.includes(id))
  const someChecked = !allChecked && selectableIds.some((id) => selected.includes(id))
  // 상세 칸은 목록의 최신 역할을 본다 (저장하면 목록을 다시 읽는다)
  const panelRole = panel?.roleId ? roles.data?.find((r) => r.id === panel.roleId) : undefined

  const openPanel = async (next: typeof panel) => {
    if (await confirmLeave()) setPanel(next)
  }

  const remove = useMutation({
    mutationFn: (ids: number[]) => api.v1.role.bulkDelete(ids),
    onSuccess: async (_, ids) => {
      setSelected([])
      if (panel?.roleId && ids.includes(panel.roleId)) setPanel(null)
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t("bulkRemoved", { count: ids.length }))
    },
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  const askRemove = async () => {
    const chosen = (roles.data ?? []).filter((r) => selected.includes(r.id))
    const ok = await confirm({
      title: t("bulkRemoveTitle", { count: chosen.length }),
      description: t("bulkRemoveDescription", { count: chosen.reduce((sum, r) => sum + r.userCount, 0) }),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate(chosen.map((r) => r.id))
  }

  const open = (role: Role) => void openPanel({ roleId: role.id })

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t.rich("guide", { link: (chunks) => <Link href="/admin/menus">{chunks}</Link> })}
      />
      {/* 넓은 폭: 두 칸이 화면 남은 높이를 채우고, 길어지면 각 칸 안에서 스크롤한다 (메뉴 관리와 같다). 목록 칸이 좁다 */}
      <div className="grid gap-4 lg:min-h-0 lg:flex-1 lg:basis-0 lg:grid-cols-[minmax(16rem,1fr)_minmax(0,2fr)] lg:grid-rows-[minmax(0,1fr)]">
        <div className="flex min-h-0 min-w-0 flex-col gap-2">
          <div className="flex items-center gap-2">
            <Input
              aria-label={t("filter")}
              placeholder={t("filterPlaceholder")}
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="min-w-0 flex-1"
            />
            <Button onClick={() => void openPanel({})}>
              <Plus />
              {t("add")}
            </Button>
          </div>
          {/* 일괄 처리 줄은 늘 같은 자리·높이 (메뉴 관리와 같다) */}
          <div
            role="toolbar"
            aria-label={t("bulkToolbar")}
            className="flex min-h-10 flex-wrap items-center gap-2 rounded-lg border bg-muted px-3 py-1"
          >
            <span className="text-sm text-muted-foreground">
              {selected.length > 0 ? t("bulkSelected", { count: selected.length }) : t("bulkHint")}
            </span>
            <span className="flex-1" />
            <Button
              size="sm"
              variant="outline"
              disabled={selected.length === 0 || remove.isPending}
              onClick={() => void askRemove()}
            >
              {t("remove")}
            </Button>
          </div>
          <QueryState query={roles} isEmpty={(d) => d.length === 0} empty={t("empty")}>
            {() => (
              <div className="min-h-0 overflow-y-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-10">
                        <Checkbox
                          aria-label={t("selectAll")}
                          checked={allChecked}
                          indeterminate={someChecked}
                          disabled={selectableIds.length === 0}
                          onCheckedChange={(checked) => setSelected(checked === true ? selectableIds : [])}
                        />
                      </TableHead>
                      <TableHead>{t("name")}</TableHead>
                      <TableHead className="w-(--col-number) text-right">{t("userCount")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {shown.map((role) => (
                      <TableRow
                        key={role.id}
                        data-state={selected.includes(role.id) ? "selected" : undefined}
                        className={cn(ROW_CLICKABLE, panel?.roleId === role.id && "bg-muted")}
                        aria-current={panel?.roleId === role.id ? "true" : undefined}
                        onClick={openOnRowClick(() => open(role))}
                      >
                        <TableCell>
                          <Checkbox
                            aria-label={t("selectOf", { name: role.name })}
                            checked={selected.includes(role.id)}
                            disabled={role.fixed}
                            onCheckedChange={(checked) =>
                              setSelected(
                                checked === true ? [...selected, role.id] : selected.filter((id) => id !== role.id),
                              )
                            }
                          />
                        </TableCell>
                        <TableCell>
                          <button
                            type="button"
                            className="flex items-center gap-2 text-left"
                            onClick={() => open(role)}
                          >
                            {role.fixed && (
                              <Lock className="size-4 text-muted-foreground" aria-label={t("fixedRole")} />
                            )}
                            {role.name}
                          </button>
                        </TableCell>
                        <TableCell className="text-right">{formatNumber(role.userCount)}</TableCell>
                      </TableRow>
                    ))}
                    {shown.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={3} className="text-center text-muted-foreground">
                          {t("filterEmpty")}
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
            )}
          </QueryState>
        </div>
        <div className="min-h-0 min-w-0">
          <PanelGuardContext.Provider value={guard}>
            {/* 고치던 역할이 목록에서 없어졌으면(다른 곳에서 지움) 칸을 비운다 */}
            {panel && (!panel.roleId || panelRole) && (
              <RolePanel
                key={panel.roleId ?? "new"}
                role={panelRole}
                open
                onOpenChange={(next) => !next && setPanel(null)}
                onCreated={(created) => setPanel({ roleId: created.id })}
              />
            )}
            {(!panel || (panel.roleId && !panelRole)) && wide && (
              <div className="hidden items-center justify-center rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground lg:flex lg:h-full">
                {t("panelEmpty")}
              </div>
            )}
          </PanelGuardContext.Provider>
        </div>
      </div>
    </>
  )
}
