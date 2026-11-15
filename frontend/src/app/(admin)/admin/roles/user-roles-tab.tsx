"use client"

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { type Column, DataTable } from "@/components/kit/data-table"
import { QueryState } from "@/components/kit/query-state"
import { KeywordFilter, SearchField, SearchPanel, SearchRow } from "@/components/kit/search-panel"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldLabel } from "@/components/ui/field"
import { useErrorMessages } from "@/lib/error-messages"
import { useListSearch } from "@/lib/list-search"
import type { RoleUser } from "@/types/role"

// 사용자별 역할 탭 (moa, specs/003-admin-roles FR-005, research R10)
//   검색어가 없으면 업무 역할이 있는 사용자와 관리자, 있으면 아이디·이름으로 모든 사용자에서 찾는다
//   관리자(ADMIN)는 모든 권한을 가져 업무 역할 칸이 "모든 권한"이고 바꾸지 않는다
//   사용자 관리 권한 없이 역할 관리 권한으로 쓴다 (FR-008)
const DEFAULTS = { keyword: "" }

export function UserRolesTab() {
  const t = useTranslations("role")
  const tu = useTranslations("userAdmin")
  const search = useListSearch({ defaults: DEFAULTS })
  const users = useQuery({
    queryKey: ["admin", "roles", "users", search.params],
    queryFn: () => api.v1.role.users(search.params),
    placeholderData: keepPreviousData,
  })
  const [target, setTarget] = useState<RoleUser>()

  const columns: Column<RoleUser>[] = [
    { key: "loginId", header: tu("loginId") },
    { key: "name", header: tu("name") },
    { key: "role", header: tu("role"), kind: "status", cell: (u) => tu(`roles.${u.role}`) },
    {
      key: "accessRoles",
      header: t("userRoles"),
      cell: (u) =>
        u.role === "ADMIN" ? (
          <span className="text-muted-foreground">{t("allPermissions")}</span>
        ) : u.accessRoles.length > 0 ? (
          u.accessRoles.map((r) => r.name).join(", ")
        ) : (
          <span className="text-muted-foreground">{t("noRoles")}</span>
        ),
    },
    {
      key: "actions",
      header: t("actions"),
      kind: "actions",
      cell: (u) =>
        u.role === "ADMIN" ? null : (
          <Button
            variant="ghost"
            size="sm"
            aria-label={t("changeRolesOf", { loginId: u.loginId })}
            onClick={() => setTarget(u)}
          >
            {t("changeRoles")}
          </Button>
        ),
    },
  ]

  return (
    <>
      <SearchPanel search={search}>
        <SearchRow>
          <SearchField label={tu("keyword")}>
            <KeywordFilter name="keyword" placeholder={tu("keywordPlaceholder")} />
          </SearchField>
        </SearchRow>
      </SearchPanel>
      <p className="text-sm text-muted-foreground">{t("userRolesTabHint")}</p>
      <DataTable columns={columns} query={users} search={search} rowKey={(u) => u.id} />
      <Dialog open={!!target} onOpenChange={(open) => !open && setTarget(undefined)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("userRoles")}</DialogTitle>
            <DialogDescription>{t("userRolesDescription", { loginId: target?.loginId ?? "" })}</DialogDescription>
          </DialogHeader>
          {target && <UserRolesForm key={target.id} target={target} onDone={() => setTarget(undefined)} />}
        </DialogContent>
      </Dialog>
    </>
  )
}

// 한 사용자의 업무 역할을 체크해 통째로 바꾼다
function UserRolesForm({ target, onDone }: { target: RoleUser; onDone: () => void }) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const [selected, setSelected] = useState<number[]>(target.accessRoles.map((r) => r.id))

  const save = useMutation({
    mutationFn: () => api.v1.role.replaceUserRoles(target.id, selected),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin"] })
      toast.success(t("userRolesSaved"))
      onDone()
    },
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  return (
    <>
      <QueryState query={roles} isEmpty={(d) => d.length === 0} empty={t("empty")}>
        {(list) => (
          <div className="flex flex-col gap-2">
            {list.map((role) => (
              <Field key={role.id} orientation="horizontal">
                <Checkbox
                  id={`user-role-${role.id}`}
                  checked={selected.includes(role.id)}
                  onCheckedChange={(checked) =>
                    setSelected(checked ? [...selected, role.id] : selected.filter((id) => id !== role.id))
                  }
                />
                <FieldLabel htmlFor={`user-role-${role.id}`} className="font-normal">
                  {role.name} <span className="font-mono text-xs text-muted-foreground">{role.code}</span>
                </FieldLabel>
              </Field>
            ))}
          </div>
        )}
      </QueryState>
      <DialogFooter>
        <Button variant="outline" onClick={onDone}>
          {t("cancel")}
        </Button>
        <Button disabled={save.isPending} onClick={() => save.mutate()}>
          {t("save")}
        </Button>
      </DialogFooter>
    </>
  )
}
