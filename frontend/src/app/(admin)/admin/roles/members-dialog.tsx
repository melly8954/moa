"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import type { Role, RoleUser } from "@/types/role"

// 역할의 구성원 (moa, specs/003-admin-roles FR-006, research R10)
//   구성원 목록과 빼기, 사용자를 찾아 더하기. 한 사람씩 더하고 뺀다 (다른 관리자가 바꾼 역할을 덮어쓰지 않는다)
//   관리자(ADMIN)는 업무 역할을 받지 않아 찾기 결과에서 더할 수 없다
export function MembersDialog({ role, onOpenChange }: { role?: Role; onOpenChange: (open: boolean) => void }) {
  const t = useTranslations("role")
  return (
    <Dialog open={!!role} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t("membersTitle", { name: role?.name ?? "" })}</DialogTitle>
          <DialogDescription>{t("membersDescription")}</DialogDescription>
        </DialogHeader>
        {role && <Members key={role.id} role={role} />}
      </DialogContent>
    </Dialog>
  )
}

function Members({ role }: { role: Role }) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const membersKey = ["admin", "roles", role.id, "members"]
  const members = useQuery({ queryKey: membersKey, queryFn: () => api.v1.role.members(role.id) })
  const [keyword, setKeyword] = useState("")
  const [searched, setSearched] = useState("")
  const found = useQuery({
    queryKey: ["admin", "roles", "users", { keyword: searched, size: 10 }],
    queryFn: () => api.v1.role.users({ keyword: searched, size: 10 }),
    enabled: searched !== "",
  })
  const memberIds = new Set((members.data ?? []).map((m) => m.id))

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "roles"] }),
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  const row = (user: RoleUser, action: React.ReactNode) => (
    <TableRow key={user.id}>
      <TableCell>{user.loginId}</TableCell>
      <TableCell>{user.name}</TableCell>
      <TableCell className="text-right">{action}</TableCell>
    </TableRow>
  )

  return (
    <div className="flex flex-col gap-4">
      <QueryState query={members} isEmpty={(d) => d.length === 0} empty={t("membersEmpty")}>
        {(list) => (
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("loginId")}</TableHead>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead className="text-right">
                    <span className="sr-only">{t("actions")}</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {list.map((m) =>
                  row(
                    m,
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={t("removeMemberOf", { loginId: m.loginId })}
                      disabled={run.isPending}
                      onClick={() => run.mutate(() => api.v1.role.removeMember(role.id, m.id))}
                    >
                      {t("removeMember")}
                    </Button>,
                  ),
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </QueryState>
      <form
        role="search"
        className="flex items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          setSearched(keyword.trim())
        }}
      >
        <Field className="flex-1">
          <FieldLabel htmlFor="member-keyword">{t("findUser")}</FieldLabel>
          <Input
            id="member-keyword"
            value={keyword}
            placeholder={t("findUserPlaceholder")}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </Field>
        <Button type="submit" variant="outline">
          {t("find")}
        </Button>
      </form>
      {searched !== "" && (
        <QueryState query={found} isEmpty={(d) => d.content.length === 0} empty={t("findEmpty")}>
          {(page) => (
            <div className="rounded-lg border">
              <Table>
                <TableBody>
                  {page.content.map((u) =>
                    row(
                      u,
                      u.role === "ADMIN" ? (
                        <span className="text-sm text-muted-foreground">{t("allPermissions")}</span>
                      ) : memberIds.has(u.id) ? (
                        <span className="text-sm text-muted-foreground">{t("alreadyMember")}</span>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          aria-label={t("addMemberOf", { loginId: u.loginId })}
                          disabled={run.isPending}
                          onClick={() => run.mutate(() => api.v1.role.addMember(role.id, u.id))}
                        >
                          {t("addMember")}
                        </Button>
                      ),
                    ),
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </QueryState>
      )}
    </div>
  )
}
