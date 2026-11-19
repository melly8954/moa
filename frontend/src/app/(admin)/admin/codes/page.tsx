"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { BulkToolbar } from "@/components/kit/bulk-toolbar"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import type { Code, CodeGroupTree } from "@/types/code"
import { CodeDrawer } from "./code-drawer"
import { CodeTreeTable, parseKey } from "./code-tree-table"

// 공통코드 (code 헬퍼, 권한 CODE_MANAGE)
// moa: 관리 화면 정리 (specs/004-admin-codes FR-004~FR-007, US1, research R5~R7). 메뉴 관리와 같은 틀이다
//   그룹은 기능이 코드에 등록한다. 화면에서 그룹을 더하지 않는다
//   그룹 > 항목 트리 표(구성 목록, 쪽 나눔 없음), 이름을 누르면 드로어, 그룹마다 "항목 추가"
//   체크박스 일괄 처리(사용·사용 안 함·삭제)는 한 그룹 안에서만. 끌어서 순서도 같은 그룹 안에서만
//   하나라도 틀리면 서버가 아무것도 바꾸지 않는다
const TREE_KEY = ["admin", "code-tree"]

export default function CodesPage() {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const tree = useQuery({ queryKey: TREE_KEY, queryFn: api.v1.code.admin.tree })
  const [editing, setEditing] = useState<{ group: CodeGroupTree; code?: Code } | null>(null)
  const [selected, setSelected] = useState<string[]>([])
  const selectedGroups = [...new Set(selected.map((key) => parseKey(key).groupCode))]
  const groupCode = selectedGroups.length === 1 ? selectedGroups[0] : undefined
  const selectedCodes = selected.map((key) => parseKey(key).code)

  // 관리 화면과 그 그룹의 화면용 목록(선택 상자)을 함께 새로 읽는다
  const refresh = (groupCodes: string[]) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: TREE_KEY }),
      ...groupCodes.map((g) => queryClient.invalidateQueries({ queryKey: ["codes", g] })),
    ])

  const run = useMutation({
    mutationFn: ({ action }: { group: string; action: () => Promise<unknown>; done?: string }) => action(),
    onSuccess: async (_, { group, done }) => {
      await refresh([group])
      if (done) toast.success(done)
    },
    onError: async (error, { group }) => {
      toast.error(describeError(error).message)
      await refresh([group])
    },
  })

  const reorder = (activeKey: string, overKey: string) => {
    const active = parseKey(activeKey)
    const over = parseKey(overKey)
    if (active.groupCode !== over.groupCode) {
      toast.error(t("dragOtherGroup"))
      return
    }
    const codes = tree.data?.find((g) => g.groupCode === active.groupCode)?.codes.map((c) => c.code) ?? []
    const next = codes.filter((c) => c !== active.code)
    next.splice(codes.indexOf(over.code), 0, active.code)
    run.mutate({
      group: active.groupCode,
      action: () =>
        api.v1.code.admin.order(
          active.groupCode,
          next.map((code, i) => ({ code, sortOrder: i })),
        ),
    })
  }

  const setEnabled = (enabled: boolean) => {
    if (!groupCode) return
    run.mutate({
      group: groupCode,
      action: () => api.v1.code.admin.setEnabled(groupCode, selectedCodes, enabled),
      done: t(enabled ? "bulkEnabled" : "bulkDisabled", { count: selected.length }),
    })
  }

  const remove = async () => {
    if (!groupCode) return
    const ok = await confirm({
      title: t("bulkRemoveTitle", { count: selected.length }),
      description: t("bulkRemoveDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(
      {
        group: groupCode,
        action: () => api.v1.code.admin.bulkDelete(groupCode, selectedCodes),
        done: t("bulkRemoved", { count: selected.length }),
      },
      { onSuccess: () => setSelected([]) },
    )
  }

  return (
    <>
      <PageHeader title={t("title")} description={t("guide")} />
      {selected.length > 0 && (
        <BulkToolbar
          label={t("bulkToolbar")}
          selected={t("bulkSelected", { count: selected.length })}
          note={groupCode ? undefined : t("bulkOneGroup")}
        >
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => setEnabled(true)}>
            {t("inUse")}
          </Button>
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => setEnabled(false)}>
            {t("notInUse")}
          </Button>
          <Button size="sm" variant="outline" disabled={run.isPending} onClick={() => void remove()}>
            {t("remove")}
          </Button>
        </BulkToolbar>
      )}
      <QueryState query={tree} isEmpty={(d) => d.length === 0} empty={t("empty")}>
        {(groups) => (
          <CodeTreeTable
            tree={groups}
            selected={selected}
            onSelectedChange={setSelected}
            onOpen={(group, code) => setEditing({ group, code })}
            onAdd={(group) => setEditing({ group })}
            onDrop={reorder}
            disabled={run.isPending}
          />
        )}
      </QueryState>
      <CodeDrawer
        key={editing ? `${editing.group.groupCode}/${editing.code?.code ?? ""}` : "closed"}
        group={editing?.group}
        code={editing?.code}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        onSaved={(g) => refresh([g])}
      />
    </>
  )
}
