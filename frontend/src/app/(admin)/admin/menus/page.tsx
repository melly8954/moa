"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ChevronDown, Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { MY_MENUS_QUERY_KEY, USER_MENUS_QUERY_KEY } from "@/components/shell/use-menu"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useErrorMessages } from "@/lib/error-messages"
import type { AdminMenu, MenuAudience } from "@/types/menu"
import { MenuDrawer } from "./menu-drawer"
import { isGroup, moveOnDrop, moveToGroup, ordersOf, structureOf } from "./menu-order"
import { MenuTreeTable } from "./menu-tree-table"

// 메뉴 관리 (menu 헬퍼, 권한 MENU_MANAGE). 셸 사이드바에 보일 메뉴 트리(2단계)
// 메뉴를 숨겨도 API는 막히지 않는다 (API는 백엔드 권한이 막는다)
// moa: 대상 탭(관리자 메뉴 / 사용자 메뉴). 탭마다 그 대상의 트리를 다룬다 (specs/001-admin-menus FR-007)
// moa: 관리 화면 정리 (FR-010~FR-012, US5, research R19~R22)
//   트리 표(구성 목록, 쪽 나눔 없음), 끌어서 순서·부모 바꾸기, 이름을 누르면 드로어, 체크박스 일괄 처리
//   일괄 처리: 사용·사용 안 함(PUT /menus/enabled), 묶음으로 옮기기(PUT /menus/order), 삭제(POST /menus/bulk-delete)
//   하나라도 틀리면 서버가 아무것도 바꾸지 않는다
export default function MenusPage() {
  const t = useTranslations("menuAdmin")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: ["admin", "menus"], queryFn: api.v1.menu.admin.tree })
  const programs = useQuery({ queryKey: ["admin", "programs"], queryFn: api.v1.menu.admin.programs })
  const [audience, setAudience] = useState<MenuAudience>("ADMIN")
  const [editing, setEditing] = useState<{ menu?: AdminMenu } | null>(null)
  const [selected, setSelected] = useState<number[]>([])
  const ofAudience = (tree.data ?? []).filter((m) => m.audience === audience)
  const groups = ofAudience.filter(isGroup)
  const all = ofAudience.flatMap((m) => [m, ...m.children])
  const selectedMenus = all.filter((m) => selected.includes(m.id))

  // 관리 화면과 셸 메뉴(관리 메뉴, 사용자 메뉴)를 함께 새로 읽는다
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin", "menus"] }),
      queryClient.invalidateQueries({ queryKey: MY_MENUS_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: USER_MENUS_QUERY_KEY }),
    ])

  const run = useMutation({
    mutationFn: ({ action }: { action: () => Promise<unknown>; done?: string }) => action(),
    onSuccess: async (_, { done }) => {
      await refresh()
      if (done) toast.success(done)
    },
    onError: async (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "B001" ? t("bulkRule") : describeError(apiError).message)
      await refresh()
    },
  })

  const reorder = (activeId: number, overId: number) => {
    const next = moveOnDrop(structureOf(ofAudience), new Set(groups.map((g) => g.id)), activeId, overId)
    if (next) run.mutate({ action: () => api.v1.menu.admin.order(ordersOf(next)) })
  }

  const setEnabled = (enabled: boolean) =>
    run.mutate({
      action: () => api.v1.menu.admin.setEnabled(selected, enabled),
      done: t(enabled ? "bulkEnabled" : "bulkDisabled", { count: selected.length }),
    })

  const moveTo = (targetId: number | null) => {
    if (selectedMenus.some(isGroup)) {
      toast.error(t("moveGroupNotAllowed"))
      return
    }
    run.mutate({
      action: () => api.v1.menu.admin.order(ordersOf(moveToGroup(structureOf(ofAudience), selected, targetId))),
      done: t("bulkMoved", { count: selected.length }),
    })
  }

  const remove = async () => {
    const ok = await confirm({
      title: t("bulkRemoveTitle", { count: selected.length }),
      description: t("bulkRemoveDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(
      { action: () => api.v1.menu.admin.bulkDelete(selected), done: t("bulkRemoved", { count: selected.length }) },
      { onSuccess: () => setSelected([]) },
    )
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t.rich("guide", { link: (chunks) => <Link href="/admin/roles">{chunks}</Link> })}
        actions={
          <Button onClick={() => setEditing({})}>
            <Plus />
            {t("add")}
          </Button>
        }
      />
      <Tabs
        value={audience}
        onValueChange={(value) => {
          setAudience(value as MenuAudience)
          setSelected([])
        }}
      >
        <TabsList aria-label={t("audience")}>
          <TabsTrigger value="ADMIN">{t("audienceAdmin")}</TabsTrigger>
          <TabsTrigger value="USER">{t("audienceUser")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {selected.length > 0 && (
        <div
          role="toolbar"
          aria-label={t("bulkToolbar")}
          className="flex flex-wrap items-center gap-2 rounded-lg border bg-muted px-3 py-2"
        >
          <span className="text-sm font-medium">{t("bulkSelected", { count: selected.length })}</span>
          <span className="flex-1" />
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => setEnabled(true)}>
            {t("inUse")}
          </Button>
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => setEnabled(false)}>
            {t("notInUse")}
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger render={<Button size="sm" variant="outline" disabled={run.isPending} />}>
              {t("moveTo")}
              <ChevronDown />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {groups.map((g) => (
                <DropdownMenuItem key={g.id} onClick={() => moveTo(g.id)}>
                  {g.name}
                </DropdownMenuItem>
              ))}
              {groups.length > 0 && <DropdownMenuSeparator />}
              <DropdownMenuItem onClick={() => moveTo(null)}>{t("topLevel")}</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => void remove()}>
            {t("remove")}
          </Button>
        </div>
      )}
      <QueryState query={tree} isEmpty={(d) => !d.some((m) => m.audience === audience)} empty={t("empty")}>
        {() => (
          <MenuTreeTable
            tree={ofAudience}
            selected={selected}
            onSelectedChange={setSelected}
            onOpen={(menu) => setEditing({ menu })}
            onDrop={reorder}
            disabled={run.isPending}
          />
        )}
      </QueryState>
      <MenuDrawer
        key={editing?.menu?.id ?? "new"}
        menu={editing?.menu}
        open={editing !== null}
        audience={audience}
        tree={ofAudience}
        programs={programs.data ?? []}
        onOpenChange={(open) => !open && setEditing(null)}
        onSaved={refresh}
      />
    </>
  )
}
