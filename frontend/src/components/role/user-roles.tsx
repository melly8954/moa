"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { create } from "zustand"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { QueryState } from "@/components/kit/query-state"
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

// 사용자에게 업무 역할 주기 (rbac 헬퍼). user-admin 헬퍼가 있으면 사용자 관리 화면의 "관리" 메뉴에 붙는다
// (user-extras.tsx의 표시 자리에 apply가 끼운다). user-admin 없이도 이 파일은 그대로 둔다
type Target = { id: number; loginId: string }

const useTargetStore = create<{ target?: Target }>(() => ({}))

// 사용자 관리 화면 "관리" 메뉴 항목
export function useUserRolesAction() {
  const t = useTranslations("role")
  return { key: "roles", label: t("userRoles"), onSelect: (user: Target) => useTargetStore.setState({ target: user }) }
}

export function UserRolesDialog() {
  const t = useTranslations("role")
  const target = useTargetStore((s) => s.target)
  const close = () => useTargetStore.setState({ target: undefined })
  return (
    <Dialog open={!!target} onOpenChange={(open) => !open && close()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("userRoles")}</DialogTitle>
          <DialogDescription>{t("userRolesDescription", { loginId: target?.loginId ?? "" })}</DialogDescription>
        </DialogHeader>
        {target && <UserRolesForm key={target.id} target={target} onDone={close} />}
      </DialogContent>
    </Dialog>
  )
}

function UserRolesForm({ target, onDone }: { target: Target; onDone: () => void }) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const given = useQuery({
    queryKey: ["admin", "users", target.id, "roles"],
    queryFn: () => api.v1.role.userRoles(target.id),
  })
  const [picked, setPicked] = useState<number[]>()
  const selected = picked ?? given.data?.map((r) => r.id) ?? []

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
                  aria-label={role.name}
                  checked={selected.includes(role.id)}
                  disabled={given.isPending}
                  onCheckedChange={(checked) =>
                    setPicked(checked ? [...selected, role.id] : selected.filter((id) => id !== role.id))
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
        <Button disabled={save.isPending || given.isPending} onClick={() => save.mutate()}>
          {t("save")}
        </Button>
      </DialogFooter>
    </>
  )
}
