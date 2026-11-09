"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ArrowDown, ArrowUp, Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { StatusText } from "@/components/kit/status-text"
import { menuIcon } from "@/components/menu/icons"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { cn } from "@/lib/utils"
import type { AdminMenu } from "@/types/menu"
import { MenuDialog } from "./menu-dialog"

// 메뉴 관리 (menu 헬퍼, 권한 MENU_MANAGE). 메뉴 트리(2단계)와 메뉴마다 볼 때 필요한 권한
// 메뉴를 숨겨도 API는 막히지 않는다 (API는 백엔드 권한이 막는다)
// 순서: 같은 단계 안에서 위·아래로 옮기면 그 단계 전체 순서를 한 번에 저장한다
export default function MenusPage() {
  const t = useTranslations("menuAdmin")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: ["admin", "menus"], queryFn: api.v1.menu.admin.tree })
  const [editing, setEditing] = useState<{ menu?: AdminMenu } | null>(null)

  // 관리 화면과 셸 메뉴(내 메뉴)를 함께 새로 읽는다
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin", "menus"] }),
      queryClient.invalidateQueries({ queryKey: ["me", "menus"] }),
    ])

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => refresh(),
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "B001" ? t("removeHasChildren") : describeError(apiError).message)
    },
  })

  const move = (siblings: AdminMenu[], index: number, step: -1 | 1) => {
    const next = [...siblings]
    const [moved] = next.splice(index, 1)
    next.splice(index + step, 0, moved)
    run.mutate(() =>
      api.v1.menu.admin.order(
        next.map((m, i) => ({ id: m.id, parentId: m.parentId ?? null, sortOrder: (i + 1) * 10 })),
      ),
    )
  }

  const remove = async (menu: AdminMenu) => {
    const ok = await confirm({
      title: t("removeTitle", { name: menu.name }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) run.mutate(() => api.v1.menu.admin.remove(menu.id))
  }

  const row = (menu: AdminMenu, siblings: AdminMenu[], index: number, child: boolean) => {
    const Icon = menuIcon(menu.icon)
    return (
      <TableRow key={menu.id}>
        <TableCell>
          <div className={cn("flex items-center gap-2", child && "pl-6")}>
            {Icon && <Icon aria-hidden="true" className="size-4 text-muted-foreground" />}
            <span className={cn(!child && "font-medium")}>{menu.name}</span>
          </div>
        </TableCell>
        <TableCell className="font-mono text-xs">{menu.path ?? t("group")}</TableCell>
        <TableCell className="font-mono text-xs">{menu.permission ?? t("everyone")}</TableCell>
        <TableCell>
          <StatusText tone={menu.enabled ? "positive" : "neutral"}>{t(menu.enabled ? "inUse" : "notInUse")}</StatusText>
        </TableCell>
        <TableCell className="text-right whitespace-nowrap">
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={t("moveUp", { name: menu.name })}
            disabled={index === 0 || run.isPending}
            onClick={() => move(siblings, index, -1)}
          >
            <ArrowUp />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={t("moveDown", { name: menu.name })}
            disabled={index === siblings.length - 1 || run.isPending}
            onClick={() => move(siblings, index, 1)}
          >
            <ArrowDown />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-label={t("editOf", { name: menu.name })}
            onClick={() => setEditing({ menu })}
          >
            {t("editShort")}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-label={t("removeOf", { name: menu.name })}
            onClick={() => void remove(menu)}
          >
            {t("remove")}
          </Button>
        </TableCell>
      </TableRow>
    )
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("pageDescription")}
        actions={
          <Button onClick={() => setEditing({})}>
            <Plus />
            {t("add")}
          </Button>
        }
      />
      <QueryState query={tree} isEmpty={(d) => d.length === 0} empty={t("empty")}>
        {(menus) => (
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("path")}</TableHead>
                  <TableHead>{t("permission")}</TableHead>
                  <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
                  <TableHead className="text-right">
                    <span className="sr-only">{t("actions")}</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {menus.flatMap((menu, i) => [
                  row(menu, menus, i, false),
                  ...menu.children.map((c, j) => row(c, menu.children, j, true)),
                ])}
              </TableBody>
            </Table>
          </div>
        )}
      </QueryState>
      <MenuDialog
        menu={editing?.menu}
        topLevel={tree.data ?? []}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        onSaved={refresh}
      />
    </>
  )
}
