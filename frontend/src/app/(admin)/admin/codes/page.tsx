"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus, Settings } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { StatusText } from "@/components/kit/status-text"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import type { Code, CodeGroupTree } from "@/types/code"
import { CodeTable } from "./code-table"
import { GroupDrawer } from "./group-drawer"
import { GroupList } from "./group-list"

// 공통코드 (code 헬퍼, 권한 CODE_MANAGE)
// moa: 왼쪽 그룹 목록 · 오른쪽 고른 그룹의 머리와 항목 표 (specs/004-admin-codes FR-004~FR-008, US1, 2026-11-21)
//   그룹 추가·설정은 드로어. 항목은 표 안에서 만들고(추가 줄) 이름을 고치고 사용을 바꾼다
//   일괄 처리(사용·사용 안 함)는 고른 그룹 안의 항목만. 다른 그룹을 고르면 체크가 풀린다
//   그룹 순서는 왼쪽 목록에서, 항목 순서는 표에서 끌어 바꾼다. 하나라도 틀리면 서버가 아무것도 바꾸지 않는다
const TREE_KEY = ["admin", "code-tree"]

type Drawer = { kind: "add" } | { kind: "edit"; group: CodeGroupTree } | null

export default function CodesPage() {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: TREE_KEY, queryFn: api.v1.code.admin.tree })
  const groups = tree.data ?? []
  const [selectedCode, setSelectedCode] = useState<string>()
  // 고른 그룹이 없거나 사라졌으면 첫 그룹
  const group = groups.find((g) => g.groupCode === selectedCode) ?? groups[0]
  const [drawer, setDrawer] = useState<Drawer>(null)
  // 체크는 그룹마다. 다른 그룹을 고르면 풀린다
  const [selection, setSelection] = useState<{ group?: string; codes: string[] }>({ codes: [] })
  const checked = selection.group === group?.groupCode ? selection.codes : []
  const none = checked.length === 0

  // 관리 화면과 그 그룹의 화면용 목록(선택 상자)을 함께 새로 읽는다
  const refresh = (groupCode?: string) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: TREE_KEY }),
      groupCode ? queryClient.invalidateQueries({ queryKey: ["codes", groupCode] }) : undefined,
    ])

  const run = useMutation({
    mutationFn: ({ action }: { group?: string; action: () => Promise<unknown>; done?: string }) => action(),
    onSuccess: async (_, { group: touched, done }) => {
      await refresh(touched)
      if (done) toast.success(done)
    },
    onError: async (error, { group: touched }) => {
      toast.error(describeError(error).message)
      await refresh(touched)
    },
  })

  const reorderGroups = (active: string, over: string) => {
    const order = groups.map((g) => g.groupCode)
    const next = order.filter((g) => g !== active)
    next.splice(order.indexOf(over), 0, active)
    run.mutate({
      action: () => api.v1.code.admin.orderGroups(next.map((groupCode, i) => ({ groupCode, sortOrder: i }))),
    })
  }

  return (
    <>
      <PageHeader title={t("title")} description={t("guide")} />
      {/* 넓은 폭: 두 칸이 화면 남은 높이를 채우고 각 칸 안에서 스크롤한다. 왼쪽 그룹 목록은 좁게, 오른쪽 그룹 상세는 넓게 */}
      <div className="grid gap-4 lg:min-h-0 lg:flex-1 lg:basis-0 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)] lg:grid-rows-[minmax(0,1fr)]">
        <section aria-labelledby="code-groups-title" className="flex min-h-0 min-w-0 flex-col rounded-lg border">
          <div className="flex items-center justify-between gap-2 border-b px-3 py-2">
            <h2 id="code-groups-title" className="text-sm font-medium">
              {t("groups")}
            </h2>
            <Button size="sm" variant="outline" onClick={() => setDrawer({ kind: "add" })}>
              <Plus />
              {t("addGroup")}
            </Button>
          </div>
          <div className="min-h-0 overflow-y-auto lg:flex-1">
            <QueryState query={tree} isEmpty={(d) => d.length === 0} empty={t("empty")}>
              {(data) => (
                <GroupList
                  groups={data}
                  selected={group?.groupCode}
                  onSelect={setSelectedCode}
                  onDrop={reorderGroups}
                  disabled={run.isPending}
                />
              )}
            </QueryState>
          </div>
        </section>
        {group && (
          <section
            aria-labelledby="code-group-title"
            className="flex min-h-0 min-w-0 flex-col gap-3 rounded-lg border p-3"
          >
            <GroupHeader group={group} onSettings={() => setDrawer({ kind: "edit", group })} />
            {/* 일괄 처리 줄은 늘 같은 자리·높이에 둔다. 고르면 표가 밀리지 않고 글자와 단추만 바뀐다 */}
            <BulkToolbar
              label={t("bulkToolbar")}
              selected={none ? t("bulkHint") : t("bulkSelected", { count: checked.length })}
            >
              {[true, false].map((enabled) => (
                <Button
                  key={String(enabled)}
                  size="sm"
                  variant="outline"
                  disabled={run.isPending || none}
                  onClick={() =>
                    run.mutate({
                      group: group.groupCode,
                      action: () => api.v1.code.admin.setEnabled(group.groupCode, { codes: checked, enabled }),
                      done: t(enabled ? "bulkEnabled" : "bulkDisabled", { count: checked.length }),
                    })
                  }
                >
                  {t(enabled ? "inUse" : "notInUse")}
                </Button>
              ))}
            </BulkToolbar>
            <div className="min-h-0 overflow-y-auto lg:flex-1">
              <CodeTable
                key={group.groupCode}
                group={group}
                checked={checked}
                onCheckedChange={(codes) => setSelection({ group: group.groupCode, codes })}
                onDrop={(active, over) => {
                  const codes = group.codes.map((c) => c.code)
                  const next = codes.filter((c) => c !== active)
                  next.splice(codes.indexOf(over), 0, active)
                  run.mutate({
                    group: group.groupCode,
                    action: () =>
                      api.v1.code.admin.order(
                        group.groupCode,
                        next.map((code, i) => ({ code, sortOrder: i })),
                      ),
                  })
                }}
                onRename={async (code: Code, name: string) => {
                  try {
                    await api.v1.code.admin.modifyCode(group.groupCode, code.code, { name, enabled: code.enabled })
                  } catch (error) {
                    toast.error(describeError(error).message)
                  }
                  await refresh(group.groupCode)
                }}
                onToggle={(code, enabled) =>
                  run.mutate({
                    group: group.groupCode,
                    action: () =>
                      api.v1.code.admin.modifyCode(group.groupCode, code.code, { name: code.name, enabled }),
                  })
                }
                onAdd={async (code, name) => {
                  try {
                    await api.v1.code.admin.createCode(group.groupCode, { code, name })
                  } catch (error) {
                    // 겹치는 코드(R002)는 추가 줄이 코드 칸에 보인다
                    if (toApiError(error).code !== "R002") toast.error(describeError(error).message)
                    throw error
                  }
                  toast.success(t("codeCreated"))
                  await refresh(group.groupCode)
                }}
                onDelete={async (code) => {
                  const ok = await confirm({
                    title: t("deleteTitle", { name: code.name }),
                    description: t("deleteDescription", { code: code.code }),
                    confirmLabel: t("delete"),
                    destructive: true,
                  })
                  if (!ok) return
                  setSelection((s) => ({ ...s, codes: s.codes.filter((c) => c !== code.code) }))
                  run.mutate({
                    group: group.groupCode,
                    action: () => api.v1.code.admin.deleteCode(group.groupCode, code.code),
                    done: t("deleted", { name: code.name }),
                  })
                }}
                disabled={run.isPending}
              />
            </div>
          </section>
        )}
      </div>
      {drawer && (
        <GroupDrawer
          key={drawer.kind === "edit" ? drawer.group.groupCode : "new"}
          group={drawer.kind === "edit" ? drawer.group : undefined}
          open
          onOpenChange={(open) => !open && setDrawer(null)}
          onSaved={async (groupCode) => {
            await refresh(groupCode)
            setSelectedCode(groupCode)
          }}
        />
      )}
    </>
  )
}

// 고른 그룹의 머리: 이름, 그룹 코드, 설명, 사용 여부, "그룹 설정"
function GroupHeader({ group, onSettings }: { group: CodeGroupTree; onSettings: () => void }) {
  const t = useTranslations("code")
  return (
    <div className="flex items-start justify-between gap-3">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <h2 id="code-group-title" className="truncate text-base font-semibold">
            {group.name}
          </h2>
          <StatusText tone={group.enabled ? "positive" : "neutral"}>
            {t(group.enabled ? "inUse" : "notInUse")}
          </StatusText>
        </div>
        <p className="mt-0.5 text-sm text-muted-foreground">
          <span className="font-mono">{group.groupCode}</span>
          {group.description && <span> · {group.description}</span>}
        </p>
      </div>
      <Button size="sm" variant="outline" onClick={onSettings} className="shrink-0">
        <Settings />
        {t("groupSettings")}
      </Button>
    </div>
  )
}
