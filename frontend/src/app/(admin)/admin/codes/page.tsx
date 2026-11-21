"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { PanelGuardContext, useIsWide, usePanelGuard } from "@/components/kit/detail-panel"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import type { Code, CodeGroupTree } from "@/types/code"
import { CodeDrawer } from "./code-drawer"
import { CodeTreeTable, groupKeyOf, parseKey } from "./code-tree-table"
import { GroupDrawer } from "./group-drawer"
import { ParentGroupField } from "./parent-group-field"

// 공통코드 (code 헬퍼, 권한 CODE_MANAGE)
// moa: 메뉴 관리와 같은 조작이다 (specs/004-admin-codes FR-004~FR-008, US1, 2026-11-21)
//   그룹 > 항목 트리 표(구성 목록, 쪽 나눔 없음), 행을 누르면 오른쪽 패널
//   트리 위 "추가" 하나로 그룹과 항목을 만든다. 패널의 "소속 그룹"이 없음이면 그룹, 그룹을 고르면 그 그룹의 항목
//   체크박스 일괄 처리(사용·사용 안 함)는 그룹과 항목을 섞어 여러 그룹에 걸쳐 한다. 그룹 체크는 그 그룹 자체를 고른다
//   그룹은 그룹끼리, 항목은 같은 그룹 안에서 끌어 순서를 바꾼다. 하나라도 틀리면 서버가 아무것도 바꾸지 않는다
const TREE_KEY = ["admin", "code-tree"]

type Panel = { kind: "code"; group: CodeGroupTree; code?: Code } | { kind: "group"; group?: CodeGroupTree } | null

export default function CodesPage() {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: TREE_KEY, queryFn: api.v1.code.admin.tree })
  const groups = tree.data ?? []
  // 오른쪽 상세 패널. 항목(code가 없으면 추가) 또는 그룹(group이 없으면 추가)
  const [panel, setPanel] = useState<Panel>(null)
  const { guard, confirmLeave } = usePanelGuard()
  // 좁은 폭은 트리 한 칸, 상세는 드로어 (FR-005)
  const wide = useIsWide()
  // 저장하지 않은 변경이 있으면 묻고 바꾼다
  const openPanel = async (next: Panel) => {
    if (await confirmLeave()) setPanel(next)
  }
  const activeKey =
    panel?.kind === "code" && panel.code
      ? `${panel.group.groupCode}/${panel.code.code}`
      : panel?.kind === "group" && panel.group
        ? groupKeyOf(panel.group.groupCode)
        : undefined
  const [selected, setSelected] = useState<string[]>([])
  const none = selected.length === 0

  // 관리 화면과 그 그룹들의 화면용 목록(선택 상자)을 함께 새로 읽는다
  const refresh = (groupCodes: string[]) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: TREE_KEY }),
      ...groupCodes.map((g) => queryClient.invalidateQueries({ queryKey: ["codes", g] })),
    ])

  const run = useMutation({
    mutationFn: ({ action }: { groups: string[]; action: () => Promise<unknown>; done?: string }) => action(),
    onSuccess: async (_, { groups: touched, done }) => {
      await refresh(touched)
      if (done) toast.success(done)
    },
    onError: async (error, { groups: touched }) => {
      toast.error(describeError(error).message)
      await refresh(touched)
    },
  })

  // 새로 만들기: 처음 소속 그룹은 지금 고른 행의 그룹, 고른 것이 없으면 없음(그룹) (FR-005)
  const currentGroup = panel?.kind === "code" ? panel.group : panel?.kind === "group" ? panel.group : undefined
  const add = () => void openPanel(currentGroup ? { kind: "code", group: currentGroup } : { kind: "group" })
  const changeParent = (groupCode: string) => {
    const group = groups.find((g) => g.groupCode === groupCode)
    setPanel(group ? { kind: "code", group } : { kind: "group" })
  }
  const parentField = (value: string) => (
    <ParentGroupField value={value} groups={groups} onChange={(next) => void changeParent(next)} />
  )

  const reorder = (activeKey: string, overKey: string) => {
    const active = parseKey(activeKey)
    const over = parseKey(overKey)
    // 그룹 끌기: 항목 위에 놓으면 그 항목의 그룹 자리로 본다
    if (active.code === undefined) {
      const order = groups.map((g) => g.groupCode)
      const next = order.filter((g) => g !== active.groupCode)
      next.splice(order.indexOf(over.groupCode), 0, active.groupCode)
      run.mutate({
        groups: [],
        action: () => api.v1.code.admin.orderGroups(next.map((groupCode, i) => ({ groupCode, sortOrder: i }))),
      })
      return
    }
    if (over.code === undefined) {
      toast.error(t("dragDropOnGroup"))
      return
    }
    if (active.groupCode !== over.groupCode) {
      toast.error(t("dragOtherGroup"))
      return
    }
    const codes = groups.find((g) => g.groupCode === active.groupCode)?.codes.map((c) => c.code) ?? []
    const next = codes.filter((c) => c !== active.code)
    next.splice(codes.indexOf(over.code), 0, active.code)
    run.mutate({
      groups: [active.groupCode],
      action: () =>
        api.v1.code.admin.order(
          active.groupCode,
          next.map((code, i) => ({ code, sortOrder: i })),
        ),
    })
  }

  const setEnabled = (enabled: boolean) => {
    const keys = selected.map(parseKey)
    const groupCodes = keys.filter((k) => k.code === undefined).map((k) => k.groupCode)
    const codes = keys.flatMap((k) => (k.code === undefined ? [] : [{ groupCode: k.groupCode, code: k.code }]))
    run.mutate({
      groups: [...new Set(keys.map((k) => k.groupCode))],
      action: () => api.v1.code.admin.setEnabledBulk({ groupCodes, codes, enabled }),
      done: t(enabled ? "bulkEnabled" : "bulkDisabled", { count: selected.length }),
    })
  }

  return (
    <>
      <PageHeader title={t("title")} description={t("guide")} />
      {/* 넓은 폭: 두 칸이 화면 남은 높이를 채우고, 길어지면 각 칸 안에서 스크롤한다 (페이지는 스크롤하지 않는다)
          basis-0으로 두어 내용 높이가 페이지를 늘리지 않게 한다 */}
      <div className="grid gap-4 lg:min-h-0 lg:flex-1 lg:basis-0 lg:grid-cols-2 lg:grid-rows-[minmax(0,1fr)]">
        <div className="flex min-h-0 min-w-0 flex-col gap-2">
          <div className="flex items-center justify-end gap-2">
            <Button size="sm" onClick={add}>
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
          </BulkToolbar>
          <QueryState query={tree} isEmpty={(d) => d.length === 0} empty={t("empty")}>
            {(data) => (
              <CodeTreeTable
                tree={data}
                className="min-h-0 overflow-y-auto lg:flex-1"
                selected={selected}
                onSelectedChange={setSelected}
                active={activeKey}
                onOpen={(group, code) => void openPanel({ kind: "code", group, code })}
                onOpenGroup={(group) => void openPanel({ kind: "group", group })}
                onDrop={reorder}
                disabled={run.isPending}
              />
            )}
          </QueryState>
        </div>
        <div className="min-h-0 min-w-0">
          <PanelGuardContext.Provider value={guard}>
            {panel?.kind === "code" && (
              <CodeDrawer
                key={`code:${panel.group.groupCode}/${panel.code?.code ?? ""}`}
                group={panel.group}
                code={panel.code}
                parentField={panel.code ? undefined : parentField(panel.group.groupCode)}
                open
                onOpenChange={(open) => !open && setPanel(null)}
                onSaved={(g) => refresh([g])}
              />
            )}
            {panel?.kind === "group" && (
              <GroupDrawer
                key={`group:${panel.group?.groupCode ?? "new"}`}
                group={panel.group}
                parentField={panel.group ? undefined : parentField("")}
                open
                onOpenChange={(open) => !open && setPanel(null)}
                onSaved={(g) => {
                  setSelected((keys) => keys.filter((key) => parseKey(key).groupCode !== g))
                  return refresh([g])
                }}
              />
            )}
            {!panel && wide && (
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
