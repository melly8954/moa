"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Lock } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { ResponsivePanel } from "@/components/kit/detail-panel"
import { useDrawerClose } from "@/components/kit/side-drawer"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { formatNumber } from "@/lib/format"
import { type RoleInput, roleSchema } from "@/lib/schemas/role"
import type { PermissionTree, PermissionTreeNode, Role } from "@/types/role"
import { PermissionTreePicker } from "./permission-tree"
import { RoleMembers } from "./role-members"

// 역할 상세 (moa, specs/003-admin-roles FR-004~FR-006·FR-010, research R19). 왼쪽 목록에서 고르거나 "역할 추가"로 연다
//   넓은 폭은 오른쪽 칸, 좁은 폭은 드로어 (ResponsivePanel, 메뉴 관리와 같다)
//   탭이 없다. 머리(이름·코드·쓸 메뉴 수·구성원 수), 이름·설명, "쓸 메뉴"와 "구성원"을 한 칸에 나란히 둔다
//   최고 관리자 역할(fixed)은 모든 권한이라 쓸 메뉴가 모두 체크된 채 꺼져 있고, 이름·설명도 바꾸지 않는다
//   내가 다룰 수 없는 역할(grantable false)은 이유를 보이고 바꾸지 않는다. 서버도 A005로 막는다
//   새 역할은 저장하기 전에는 구성원을 더할 수 없다. 저장하면 onCreated로 그 역할을 다시 연다
export function RolePanel({
  role,
  open,
  onOpenChange,
  onCreated,
}: {
  role?: Role
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated: (role: Role) => void
}) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!role
  const locked = !!role && (role.fixed || !role.grantable)
  const tree = useQuery({
    queryKey: ["admin", "roles", "permission-tree"],
    queryFn: api.v1.role.permissionTree,
    enabled: open,
  })
  const [formError, setFormError] = useState<string>()
  const form = useForm<RoleInput>({
    resolver: zodResolver(roleSchema),
    values: {
      code: role?.code ?? "",
      name: role?.name ?? "",
      description: role?.description ?? "",
      permissions: role?.permissions ?? [],
    },
  })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)

  const save = useMutation({
    mutationFn: ({ code, name, description, permissions: keys }: RoleInput) =>
      editing
        ? api.v1.role.modify(role.id, { name, description: description || undefined, permissions: keys })
        : api.v1.role.create({ code, name, description: description || undefined, permissions: keys }),
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t(editing ? "modified" : "created"))
      form.reset()
      if (!editing) onCreated(saved)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("code", { message: "role.duplicate" })
      for (const fieldError of apiError.fieldErrors) form.setError(fieldError.field as keyof RoleInput, { message: "" })
      // A005: 내가 가진 권한 밖의 권한, 사용자 관리 권한(최고 관리자만)을 넣거나 뺐다 (권한 상승 방지)
      setFormError(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  const permissionCount = role?.fixed && tree.data ? allKeys(tree.data).length : (role?.permissions.length ?? 0)

  return (
    <ResponsivePanel
      open={open}
      onOpenChange={(next) => {
        if (!next) setFormError(undefined)
        onOpenChange(next)
      }}
      // 칸의 이름(region)은 글자 제목에서 온다. 자물쇠는 설명 줄에 둔다
      title={editing ? role.name : t("add")}
      description={
        editing ? (
          <span className="flex flex-wrap items-center gap-x-3">
            {role.fixed && (
              <span className="flex items-center gap-1">
                <Lock className="size-3.5" aria-hidden />
                {t("fixedRole")}
              </span>
            )}
            <span className="font-mono">{role.code}</span>
            <span>{t("permissionCountOf", { count: formatNumber(permissionCount) })}</span>
            <span>{t("userCountOf", { count: formatNumber(role.userCount) })}</span>
          </span>
        ) : (
          t("dialogDescription")
        )
      }
      dirty={form.formState.isDirty}
      footer={
        locked ? undefined : (
          <>
            <Button type="button" variant="outline" onClick={() => void close()}>
              {t("cancel")}
            </Button>
            <Button type="submit" form="role-form" disabled={save.isPending}>
              {t("save")}
            </Button>
          </>
        )
      }
    >
      {role?.fixed && (
        <Alert>
          <AlertDescription>{t("fixedHint")}</AlertDescription>
        </Alert>
      )}
      {role && !role.fixed && !role.grantable && (
        <Alert>
          <AlertDescription>
            {t(role.blockReason === "SUPER_ADMIN_ONLY" ? "blockSuperAdminOnly" : "blockWider")}
          </AlertDescription>
        </Alert>
      )}
      <form
        id="role-form"
        noValidate
        onSubmit={form.handleSubmit((values) => {
          setFormError(undefined)
          save.mutate(values)
        })}
      >
        <FieldGroup>
          {formError && (
            <Alert variant="destructive">
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          {(editing ? (["name", "description"] as const) : (["code", "name", "description"] as const)).map((name) => (
            <Controller
              key={name}
              name={name}
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`role-${name}`}>{t(name)}</FieldLabel>
                  <Input
                    {...field}
                    id={`role-${name}`}
                    disabled={locked}
                    aria-invalid={fieldState.invalid}
                    aria-describedby={fieldState.invalid ? `role-${name}-error` : undefined}
                  />
                  {fieldState.invalid && (
                    <FieldError id={`role-${name}-error`} errors={fieldErrorText(fieldState.error)} />
                  )}
                </Field>
              )}
            />
          ))}
        </FieldGroup>
      </form>
      {/* 쓸 메뉴와 구성원을 나란히 (좁은 폭의 드로어 안에서는 위아래) */}
      <div className="grid gap-6 xl:grid-cols-2">
        <div className="min-w-0">
          <Controller
            name="permissions"
            control={form.control}
            render={({ field }) =>
              tree.data ? (
                <PermissionTreePicker
                  tree={tree.data}
                  value={role?.fixed ? allKeys(tree.data) : field.value}
                  onChange={field.onChange}
                  disabled={locked}
                />
              ) : (
                <></>
              )
            }
          />
        </div>
        <section aria-label={t("tabMembers")} className="flex min-w-0 flex-col gap-2">
          <h3 className="text-sm font-medium">{t("tabMembers")}</h3>
          {editing ? (
            <RoleMembers role={role} />
          ) : (
            <p className="text-sm text-muted-foreground">{t("membersAfterSave")}</p>
          )}
        </section>
      </div>
    </ResponsivePanel>
  )
}

// 트리의 모든 권한 키 (최고 관리자 역할은 모두 가진다)
function allKeys(tree: PermissionTree): string[] {
  const keysOf = (node: PermissionTreeNode): string[] =>
    node.permission ? [node.permission.key] : node.children.flatMap(keysOf)
  return [...new Set([...tree.menus.flatMap(keysOf), ...tree.unlisted.map((s) => s.permission.key)])]
}
