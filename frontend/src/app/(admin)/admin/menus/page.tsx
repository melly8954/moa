"use client"

import { type UseQueryResult, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
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
import { MY_MENUS_QUERY_KEY, USER_MENUS_QUERY_KEY } from "@/components/shell/use-menu"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useErrorMessages } from "@/lib/error-messages"
import { cn } from "@/lib/utils"
import type { AdminMenu, MenuAudience, Program } from "@/types/menu"
import { MenuDialog } from "./menu-dialog"

// 메뉴 관리 (menu 헬퍼, 권한 MENU_MANAGE). 메뉴 트리(2단계)
// 메뉴를 숨겨도 API는 막히지 않는다 (API는 백엔드 권한이 막는다)
// 순서: 같은 단계 안에서 위·아래로 옮기면 그 단계 전체 순서를 한 번에 저장한다
// moa: 대상 탭(관리자 메뉴 / 사용자 메뉴). 탭마다 그 대상의 트리와 순서를 다룬다 (specs/001-admin-menus FR-007, research R11)
// moa: 프로그램 탭은 메뉴에 걸 수 있는 화면 목록이다. 코드에 등록하므로 조회만 한다 (FR-008, research R16)
//   메뉴 목록은 경로·권한 키 대신 프로그램과 "보이는 사람"을 보인다
export default function MenusPage() {
  const t = useTranslations("menuAdmin")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: ["admin", "menus"], queryFn: api.v1.menu.admin.tree })
  const [editing, setEditing] = useState<{ menu?: AdminMenu } | null>(null)
  const programs = useQuery({ queryKey: ["admin", "programs"], queryFn: api.v1.menu.admin.programs })
  const [tab, setTab] = useState<MenuAudience | "PROGRAMS">("ADMIN")
  const audience: MenuAudience = tab === "USER" ? "USER" : "ADMIN"
  const ofAudience = (tree.data ?? []).filter((m) => m.audience === audience)

  // 관리 화면과 셸 메뉴(관리 메뉴, 사용자 메뉴)를 함께 새로 읽는다
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin", "menus"] }),
      queryClient.invalidateQueries({ queryKey: MY_MENUS_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: USER_MENUS_QUERY_KEY }),
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

  // 사이드바에 누구에게 보이나. 관리 메뉴는 프로그램의 권한, 사용자 메뉴는 공개 범위, 묶음 메뉴는 하위 메뉴를 따른다
  const whoSees = (menu: AdminMenu) => {
    if (!menu.programKey) return <span className="text-muted-foreground">{t("followsChildren")}</span>
    if (!menu.program) return <span className="text-muted-foreground">{t("hidden")}</span>
    if (menu.audience === "ADMIN") return menu.program.permissionName
    return t(menu.visibility === "MEMBER" ? "visibilityMember" : "visibilityPublic")
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
        <TableCell>
          {menu.program ? (
            <div className="flex flex-col">
              <span>{menu.program.name}</span>
              <span className="font-mono text-xs text-muted-foreground">{menu.program.path}</span>
            </div>
          ) : menu.missingProgram ? (
            <StatusText tone="negative">{t("missingProgram")}</StatusText>
          ) : (
            <span className="text-muted-foreground">{t("group")}</span>
          )}
        </TableCell>
        <TableCell>{whoSees(menu)}</TableCell>
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
          tab !== "PROGRAMS" && (
            <Button onClick={() => setEditing({})}>
              <Plus />
              {t("add")}
            </Button>
          )
        }
      />
      <Tabs value={tab} onValueChange={(value) => setTab(value as MenuAudience | "PROGRAMS")}>
        <TabsList aria-label={t("audience")}>
          <TabsTrigger value="ADMIN">{t("audienceAdmin")}</TabsTrigger>
          <TabsTrigger value="USER">{t("audienceUser")}</TabsTrigger>
          <TabsTrigger value="PROGRAMS">{t("programs")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "PROGRAMS" ? (
        <ProgramTable query={programs} />
      ) : (
        <QueryState query={tree} isEmpty={(d) => !d.some((m) => m.audience === audience)} empty={t("empty")}>
          {() => (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("name")}</TableHead>
                    <TableHead>{t("program")}</TableHead>
                    <TableHead>{t("whoSees")}</TableHead>
                    <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
                    <TableHead className="text-right">
                      <span className="sr-only">{t("actions")}</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ofAudience.flatMap((menu, i) => [
                    row(menu, ofAudience, i, false),
                    ...menu.children.map((c, j) => row(c, menu.children, j, true)),
                  ])}
                </TableBody>
              </Table>
            </div>
          )}
        </QueryState>
      )}
      <MenuDialog
        menu={editing?.menu}
        audience={audience}
        topLevel={ofAudience}
        programs={programs.data ?? []}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        onSaved={refresh}
      />
    </>
  )
}

// 프로그램 목록 (조회만). 고치거나 더하는 것은 코드에서 한다
function ProgramTable({ query }: { query: UseQueryResult<Program[]> }) {
  const t = useTranslations("menuAdmin")
  return (
    <QueryState query={query} isEmpty={(d) => d.length === 0} empty={t("programsEmpty")}>
      {(list) => (
        <>
          <p className="text-sm text-muted-foreground">{t("programsDescription")}</p>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("path")}</TableHead>
                  <TableHead>{t("audience")}</TableHead>
                  <TableHead>{t("whoCanUse")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {list.map((p) => (
                  <TableRow key={p.key}>
                    <TableCell>{p.name}</TableCell>
                    <TableCell className="font-mono text-xs">{p.path}</TableCell>
                    <TableCell>{t(p.audience === "USER" ? "audienceUser" : "audienceAdmin")}</TableCell>
                    <TableCell>
                      {p.permission
                        ? t("programPermission", { name: p.permission.name })
                        : t(p.signInRequired ? "programSignIn" : "programPublic")}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </QueryState>
  )
}
