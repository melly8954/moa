"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { formatNumber } from "@/lib/format"
import type { Role } from "@/types/role"
import { RoleDrawer, type RoleDrawerTab } from "./role-drawer"

// 역할 관리 (rbac 헬퍼, 권한 ROLE_MANAGE). 역할에 권한을 묶고, 구성원에게 역할을 준다
// moa: 관리 화면 정리 (specs/003-admin-roles FR-005~FR-009, research R12)
//   역할 목록 하나(탭 없음). 구성 목록이라 쪽 나눔 없이 전체를 보이고 이름·코드 검색은 화면에서 거른다
//   행을 누르면 드로어(기본·메뉴 / 구성원), 체크박스로 골라 한꺼번에 지운다
//   사용자에게 역할을 주는 곳은 드로어의 구성원 탭뿐이다 (사용자 관리는 보기만, 002 FR-005)
export default function RolesPage() {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const [filter, setFilter] = useState("")
  const [selected, setSelected] = useState<number[]>([])
  const [drawer, setDrawer] = useState<{ roleId?: number; tab: RoleDrawerTab } | null>(null)
  const keyword = filter.trim().toLowerCase()
  const shown = (roles.data ?? []).filter(
    (r) => !keyword || r.name.toLowerCase().includes(keyword) || r.code.toLowerCase().includes(keyword),
  )
  const shownIds = shown.map((r) => r.id)
  const allChecked = shownIds.length > 0 && shownIds.every((id) => selected.includes(id))
  const someChecked = !allChecked && shownIds.some((id) => selected.includes(id))
  // 드로어는 목록의 최신 역할을 본다 (저장하면 목록을 다시 읽는다)
  const drawerRole = roles.data?.find((r) => r.id === drawer?.roleId)

  const remove = useMutation({
    mutationFn: (ids: number[]) => api.v1.role.bulkDelete(ids),
    onSuccess: async (_, ids) => {
      setSelected([])
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

  const open = (role: Role) => setDrawer({ roleId: role.id, tab: "basic" })

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t.rich("guide", { link: (chunks) => <Link href="/admin/menus">{chunks}</Link> })}
        actions={
          <Button onClick={() => setDrawer({ tab: "basic" })}>
            <Plus />
            {t("add")}
          </Button>
        }
      />
      <Input
        aria-label={t("filter")}
        placeholder={t("filterPlaceholder")}
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        className="max-w-sm"
      />
      {selected.length > 0 && (
        <div
          role="toolbar"
          aria-label={t("bulkToolbar")}
          className="flex flex-wrap items-center gap-2 rounded-lg border bg-muted px-3 py-2"
        >
          <span className="text-sm font-medium">{t("bulkSelected", { count: selected.length })}</span>
          <span className="flex-1" />
          <Button size="sm" variant="outline" disabled={remove.isPending} onClick={() => void askRemove()}>
            {t("remove")}
          </Button>
        </div>
      )}
      <QueryState query={roles} isEmpty={(d) => d.length === 0} empty={t("empty")}>
        {() => (
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-10">
                    <Checkbox
                      aria-label={t("selectAll")}
                      checked={allChecked}
                      indeterminate={someChecked}
                      onCheckedChange={(checked) => setSelected(checked === true ? shownIds : [])}
                    />
                  </TableHead>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("code")}</TableHead>
                  <TableHead className="w-(--col-number) text-right">{t("permissionCount")}</TableHead>
                  <TableHead className="w-(--col-number) text-right">{t("userCount")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {shown.map((role) => (
                  <TableRow key={role.id} data-state={selected.includes(role.id) ? "selected" : undefined}>
                    <TableCell>
                      <Checkbox
                        aria-label={t("selectOf", { name: role.name })}
                        checked={selected.includes(role.id)}
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
                        className="text-left underline-offset-4 hover:underline"
                        onClick={() => open(role)}
                      >
                        {role.name}
                      </button>
                    </TableCell>
                    <TableCell className="font-mono">{role.code}</TableCell>
                    <TableCell className="text-right">{formatNumber(role.permissions.length)}</TableCell>
                    <TableCell className="text-right">{formatNumber(role.userCount)}</TableCell>
                  </TableRow>
                ))}
                {shown.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground">
                      {t("filterEmpty")}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </QueryState>
      <RoleDrawer
        key={drawer?.roleId ?? "new"}
        role={drawerRole}
        open={drawer !== null}
        tab={drawer?.tab ?? "basic"}
        onTabChange={(tab) => setDrawer((prev) => (prev ? { ...prev, tab } : prev))}
        onOpenChange={(next) => !next && setDrawer(null)}
        onCreated={(created) => setDrawer({ roleId: created.id, tab: "members" })}
      />
    </>
  )
}
