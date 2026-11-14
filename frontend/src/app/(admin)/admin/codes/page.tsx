"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { StatusText } from "@/components/kit/status-text"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { cn } from "@/lib/utils"
import type { Code } from "@/types/code"
import { CodeDialog, GroupDialog } from "./code-dialogs"

// 코드 관리 (code 헬퍼, 권한 CODE_MANAGE). 왼쪽 그룹 목록, 오른쪽 고른 그룹의 코드
// 고른 그룹은 주소(?group=)에 둔다. 그룹 이름 바꾸기·지우기는 백엔드에 없다
export default function CodesPage() {
  const t = useTranslations("code")
  const router = useRouter()
  const pathname = usePathname()
  const selected = useSearchParams().get("group") ?? ""
  const groups = useQuery({
    queryKey: ["admin", "code-groups"],
    queryFn: api.v1.code.admin.groups,
  })
  const [groupOpen, setGroupOpen] = useState(false)

  const select = (groupCode: string) =>
    router.replace(`${pathname}?group=${encodeURIComponent(groupCode)}`, {
      scroll: false,
    })

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("pageDescription")}
        actions={
          <Button onClick={() => setGroupOpen(true)}>
            <Plus />
            {t("addGroup")}
          </Button>
        }
      />
      <div className="grid gap-(--page-gap) lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <Card>
          <CardHeader>
            <CardTitle>
              <h2>{t("groups")}</h2>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <QueryState query={groups} isEmpty={(d) => d.length === 0} empty={t("noGroups")}>
              {(list) => (
                <ul className="flex flex-col gap-1">
                  {list.map((group) => (
                    <li key={group.groupCode}>
                      <Button
                        variant={group.groupCode === selected ? "secondary" : "ghost"}
                        className="h-auto w-full flex-col items-start gap-0 py-2 text-left"
                        aria-current={group.groupCode === selected ? "true" : undefined}
                        onClick={() => select(group.groupCode)}
                      >
                        <span className="font-medium">{group.name}</span>
                        {/* 고른 단추 바탕(secondary) 위에서는 흐린 글자의 대비가 모자라 기본 글자색을 쓴다 */}
                        <span
                          className={cn("font-mono text-xs", group.groupCode !== selected && "text-muted-foreground")}
                        >
                          {group.groupCode}
                        </span>
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </QueryState>
          </CardContent>
        </Card>
        {selected ? (
          <CodeList groupCode={selected} groupName={groups.data?.find((g) => g.groupCode === selected)?.name} />
        ) : (
          <Card>
            <CardContent className="text-sm text-muted-foreground">{t("pickGroup")}</CardContent>
          </Card>
        )}
      </div>
      <GroupDialog open={groupOpen} onOpenChange={setGroupOpen} onCreated={select} />
    </>
  )
}

function CodeList({ groupCode, groupName }: { groupCode: string; groupName?: string }) {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const codes = useQuery({
    queryKey: ["admin", "codes", groupCode],
    queryFn: () => api.v1.code.admin.codes(groupCode),
  })
  const [editing, setEditing] = useState<{ code?: Code } | null>(null)

  const remove = useMutation({
    mutationFn: (code: string) => api.v1.code.admin.removeCode(groupCode, code),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["admin", "codes", groupCode],
        }),
        queryClient.invalidateQueries({ queryKey: ["codes", groupCode] }),
      ])
      toast.success(t("codeRemoved"))
    },
    onError: (error) => toast.error(describeError(error).message),
  })

  const askRemove = async (code: Code) => {
    const ok = await confirm({
      title: t("removeTitle", { name: code.name }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate(code.code)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2>{groupName ? t("codesOf", { name: groupName }) : groupCode}</h2>
        </CardTitle>
        <CardAction>
          <Button size="sm" onClick={() => setEditing({})}>
            <Plus />
            {t("addCode")}
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        <QueryState query={codes} isEmpty={(d) => d.length === 0} empty={t("noCodes")}>
          {(list) => (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("code")}</TableHead>
                    <TableHead>{t("name")}</TableHead>
                    <TableHead className="w-(--col-number) text-right">{t("sortOrder")}</TableHead>
                    <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
                    <TableHead className="w-(--col-actions) text-right">
                      <span className="sr-only">{t("actions")}</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {list.map((code) => (
                    <TableRow key={code.code}>
                      <TableCell>
                        <div className="max-w-(--col-text-max) truncate font-mono">{code.code}</div>
                      </TableCell>
                      <TableCell>
                        <div className="max-w-(--col-text-max) truncate">{code.name}</div>
                      </TableCell>
                      <TableCell className="text-right">{code.sortOrder}</TableCell>
                      <TableCell>
                        <StatusText tone={code.enabled ? "positive" : "neutral"}>
                          {t(code.enabled ? "inUse" : "notInUse")}
                        </StatusText>
                      </TableCell>
                      <TableCell className={cn("text-right")}>
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={t("editOf", { name: code.name })}
                          onClick={() => setEditing({ code })}
                        >
                          {t("edit")}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={t("removeOf", { name: code.name })}
                          onClick={() => void askRemove(code)}
                        >
                          {t("remove")}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </QueryState>
      </CardContent>
      <CodeDialog
        groupCode={groupCode}
        code={editing?.code}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
      />
    </Card>
  )
}
