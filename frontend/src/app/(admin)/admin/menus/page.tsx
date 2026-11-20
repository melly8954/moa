"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ChevronDown, Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { PanelGuardContext, useIsWide, usePanelGuard } from "@/components/kit/detail-panel"
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
//   트리 표(구성 목록, 쪽 나눔 없음), 끌어서 순서·부모 바꾸기, 체크박스 일괄 처리
//   일괄 처리: 사용·사용 안 함(PUT /menus/enabled), 상위 메뉴로 옮기기(PUT /menus/order), 삭제(POST /menus/bulk-delete)
//   하나라도 틀리면 서버가 아무것도 바꾸지 않는다
// moa: 두 칸 화면 (FR-010, US5, research R25). 공통코드와 같은 틀이다
//   넓은 폭: 왼쪽 탭·일괄 처리 줄·트리, 오른쪽 상세 칸(이름을 누르거나 "메뉴 추가"). 좁은 폭: 트리 한 칸, 상세는 드로어
//   저장하지 않은 변경이 있으면 다른 행·대상 탭·메뉴 추가로 가기 전에 묻는다
export default function MenusPage() {
  const t = useTranslations("menuAdmin")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: ["admin", "menus"], queryFn: api.v1.menu.admin.tree })
  const screens = useQuery({ queryKey: ["admin", "screens"], queryFn: api.v1.menu.admin.screens })
  const [audience, setAudience] = useState<MenuAudience>("ADMIN")
  // 오른쪽 상세 칸: 고친다(menu) 또는 새로 만든다(menu 없음, parentId는 상위 메뉴 칸의 처음 값)
  const [panel, setPanel] = useState<{ menu?: AdminMenu; parentId?: number } | null>(null)
  const { guard, confirmLeave } = usePanelGuard()
  const wide = useIsWide()
  const [selected, setSelected] = useState<number[]>([])
  const ofAudience = (tree.data ?? []).filter((m) => m.audience === audience)
  const groups = ofAudience.filter(isGroup)
  const all = ofAudience.flatMap((m) => [m, ...m.children])
  const selectedMenus = all.filter((m) => selected.includes(m.id))
  // 고친 뒤 트리를 다시 읽으면 상세 칸도 새 값을 보인다
  const panelMenu = panel?.menu ? all.find((m) => m.id === panel.menu?.id) : undefined

  // 저장하지 않은 변경이 있으면 묻고 바꾼다
  const openPanel = async (next: typeof panel) => {
    if (await confirmLeave()) setPanel(next)
  }

  // 새 메뉴의 상위 메뉴 처음 값: 고른 메뉴가 상위 메뉴면 그 메뉴, 하위 메뉴면 그 부모, 아니면 맨 위 (FR-010)
  const addMenu = () => {
    const current = panelMenu
    const parentId = current?.parentId ?? (current && isGroup(current) ? current.id : undefined)
    void openPanel({ parentId })
  }

  const changeAudience = async (value: MenuAudience) => {
    if (!(await confirmLeave())) return
    setAudience(value)
    setSelected([])
    setPanel(null)
  }

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
      {
        onSuccess: () => {
          if (panel?.menu && selected.includes(panel.menu.id)) setPanel(null)
          setSelected([])
        },
      },
    )
  }

  const none = selected.length === 0

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t.rich("guide", { link: (chunks) => <Link href="/admin/roles">{chunks}</Link> })}
      />
      {/* 넓은 폭: 두 칸이 화면 남은 높이를 채우고, 길어지면 각 칸 안에서 스크롤한다 (공통코드와 같다) */}
      <div className="grid gap-4 lg:min-h-0 lg:flex-1 lg:basis-0 lg:grid-cols-2 lg:grid-rows-[minmax(0,1fr)]">
        <div className="flex min-h-0 min-w-0 flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <Tabs value={audience} onValueChange={(value) => void changeAudience(value as MenuAudience)}>
              <TabsList aria-label={t("audience")}>
                <TabsTrigger value="ADMIN">{t("audienceAdmin")}</TabsTrigger>
                <TabsTrigger value="USER">{t("audienceUser")}</TabsTrigger>
              </TabsList>
            </Tabs>
            <span className="flex-1" />
            <Button size="sm" onClick={addMenu}>
              <Plus />
              {t("add")}
            </Button>
          </div>
          {/* 일괄 처리 줄은 늘 같은 자리·높이에 둔다. 고르면 목록이 밀리지 않고 글자와 단추만 바뀐다 */}
          <BulkToolbar
            label={t("bulkToolbar")}
            selected={none ? t("bulkHint") : t("bulkSelected", { count: selected.length })}
          >
            <Button size="sm" variant="outline" disabled={run.isPending || none} onClick={() => setEnabled(true)}>
              {t("inUse")}
            </Button>
            <Button size="sm" variant="outline" disabled={run.isPending || none} onClick={() => setEnabled(false)}>
              {t("notInUse")}
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger render={<Button size="sm" variant="outline" disabled={run.isPending || none} />}>
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
            <Button size="sm" variant="outline" disabled={run.isPending || none} onClick={() => void remove()}>
              {t("remove")}
            </Button>
          </BulkToolbar>
          <QueryState query={tree} isEmpty={(d) => !d.some((m) => m.audience === audience)} empty={t("empty")}>
            {() => (
              <MenuTreeTable
                tree={ofAudience}
                className="min-h-0 overflow-y-auto lg:flex-1"
                selected={selected}
                onSelectedChange={setSelected}
                active={panelMenu?.id}
                onOpen={(menu) => void openPanel({ menu })}
                onDrop={reorder}
                disabled={run.isPending}
              />
            )}
          </QueryState>
        </div>
        <div className="min-h-0 min-w-0">
          <PanelGuardContext.Provider value={guard}>
            {/* 고치던 메뉴가 트리에서 없어졌으면(다른 곳에서 지움) 칸을 비운다 */}
            {panel && (!panel.menu || panelMenu) && (
              <MenuDrawer
                key={panel.menu ? `menu:${panel.menu.id}` : `new:${audience}:${panel.parentId ?? ""}`}
                menu={panelMenu}
                open
                audience={audience}
                tree={ofAudience}
                screens={screens.data ?? []}
                defaultParentId={panel.parentId}
                onOpenChange={(open) => !open && setPanel(null)}
                onSaved={refresh}
              />
            )}
            {(!panel || (panel.menu && !panelMenu)) && wide && (
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
