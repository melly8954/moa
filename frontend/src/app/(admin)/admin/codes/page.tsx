"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
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
import { GroupDrawer } from "./group-drawer"

// 공통코드 (code 헬퍼, 권한 CODE_MANAGE)
// moa: 관리 화면 정리 (specs/004-admin-codes FR-004~FR-008, US1, research R7·R9). 메뉴 관리와 같은 틀이다
//   그룹은 머리의 "그룹 추가"와 그룹 이름(그룹 드로어)으로 다룬다. 시스템 그룹은 지울 수 없다
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
  // 그룹 드로어: group이 없으면 추가
  const [editingGroup, setEditingGroup] = useState<{ group?: CodeGroupTree } | null>(null)
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
      <PageHeader
        title={t("title")}
        description={t("guide")}
        actions={
          <Button onClick={() => setEditingGroup({})}>
            <Plus />
            {t("addGroup")}
          </Button>
        }
      />
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
            onOpenGroup={(group) => setEditingGroup({ group })}
            onAdd={(group) => setEditing({ group })}
            onDrop={reorder}
            disabled={run.isPending}
          />
        )}
      </QueryState>
      <CodeDrawer
        key={editing ? `code:${editing.group.groupCode}/${editing.code?.code ?? ""}` : "code:closed"}
        group={editing?.group}
        code={editing?.code}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        onSaved={(g) => refresh([g])}
      />
      <GroupDrawer
        key={editingGroup ? `group:${editingGroup.group?.groupCode ?? "new"}` : "group:closed"}
        group={editingGroup?.group}
        open={editingGroup !== null}
        onOpenChange={(open) => !open && setEditingGroup(null)}
        onSaved={(g) => {
          setSelected((keys) => keys.filter((key) => parseKey(key).groupCode !== g))
          return refresh([g])
        }}
      />
    </>
  )
}
