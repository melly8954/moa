"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { StatusText } from "@/components/kit/status-text"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { useFieldErrorText } from "@/i18n/field-error"
import { formatDateTime } from "@/lib/format"
import { type UserCreateInput, userCreateSchema } from "@/lib/schemas/user-admin"
import type { AdminUser } from "@/types/user-admin"
import { ROLES, TemporaryPasswordPanel, useInvalidateUsers, useUserErrorText } from "./user-dialogs"
import type { UserAction } from "./user-extras"

// 사용자 드로어 (moa, specs/002-admin-users FR-007, US3, research R7). 목록에서 행을 누르면 열린다
//   위: 아이디·이름·시스템 역할·상태·업무 역할(보기만, 역할 관리 링크)·만든 시각
//   아래: 이름 바꾸기·시스템 역할 바꾸기·잠그기·비밀번호 초기화·삭제 (막는 규칙은 목록에 있던 그대로)
//   계정을 바꾸는 일은 한 사람씩 확인하므로 일괄 처리는 두지 않는다
export function UserDrawer({
  user,
  onClose,
  protectedRow,
  isSelf,
  iAmAdmin,
  extraActions,
  onRename,
  onChangeRole,
  onToggleLock,
  onResetPassword,
  onRemove,
}: {
  user?: AdminUser
  onClose: () => void
  protectedRow: boolean
  isSelf: boolean
  iAmAdmin: boolean
  extraActions: UserAction[]
  onRename: () => void
  onChangeRole: () => void
  onToggleLock: () => void
  onResetPassword: () => void
  onRemove: () => void
}) {
  const t = useTranslations("userAdmin")

  return (
    <SideDrawer
      open={!!user}
      onOpenChange={(open) => !open && onClose()}
      title={user?.loginId ?? ""}
      description={t("drawerDescription")}
    >
      {user && (
        <>
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 text-sm">
            <dt className="text-muted-foreground">{t("loginId")}</dt>
            <dd>{user.loginId}</dd>
            <dt className="text-muted-foreground">{t("name")}</dt>
            <dd>{user.name}</dd>
            <dt className="text-muted-foreground">{t("role")}</dt>
            <dd>{t(`roles.${user.role}`)}</dd>
            <dt className="text-muted-foreground">{t("status")}</dt>
            <dd>
              <UserStatus user={user} />
            </dd>
            <dt className="text-muted-foreground">{t("accessRoles")}</dt>
            <dd className="flex flex-col gap-1">
              <span>
                {user.role === "ADMIN"
                  ? t("allPermissions")
                  : user.accessRoles.length > 0
                    ? user.accessRoles.map((r) => r.name).join(", ")
                    : t("noAccessRoles")}
              </span>
              {user.role !== "ADMIN" && (
                <span className="text-muted-foreground">
                  {t.rich("accessRolesHint", {
                    link: (chunks) => (
                      <Link href="/admin/roles" className="text-foreground underline underline-offset-4">
                        {chunks}
                      </Link>
                    ),
                  })}
                </span>
              )}
            </dd>
            <dt className="text-muted-foreground">{t("createdAt")}</dt>
            <dd>{formatDateTime(user.createdAt)}</dd>
          </dl>
          <div className="flex flex-col gap-2 border-t pt-4">
            <Button variant="outline" disabled={protectedRow} onClick={onRename}>
              {t("rename")}
            </Button>
            <Button variant="outline" disabled={protectedRow || isSelf || !iAmAdmin} onClick={onChangeRole}>
              {t("changeRole")}
            </Button>
            <Button variant="outline" disabled={protectedRow || isSelf} onClick={onToggleLock}>
              {t(user.locked ? "unlock" : "lock")}
            </Button>
            <Button variant="outline" disabled={protectedRow || !user.hasPassword} onClick={onResetPassword}>
              {t("resetPassword")}
            </Button>
            {extraActions.map((action) => (
              <Button
                key={action.key}
                variant="outline"
                disabled={protectedRow || action.disabled?.(user)}
                onClick={() => action.onSelect(user)}
              >
                {action.label}
              </Button>
            ))}
            <Button variant="destructive" disabled={protectedRow || isSelf} onClick={onRemove}>
              {t("remove")}
            </Button>
          </div>
        </>
      )}
    </SideDrawer>
  )
}

export function UserStatus({ user }: { user: AdminUser }) {
  const t = useTranslations("userAdmin")
  return user.locked ? (
    <StatusText tone="negative">{t("statusLocked")}</StatusText>
  ) : user.passwordChangeRequired ? (
    <StatusText tone="neutral">{t("statusPasswordPending")}</StatusText>
  ) : (
    <StatusText tone="positive">{t("statusActive")}</StatusText>
  )
}

// 사용자 만들기 드로어 (moa, FR-007). 저장 직후 임시 비밀번호를 드로어 안에 한 번 보이고, 닫으면 다시 볼 수 없다
export function CreateUserDrawer({
  open,
  onOpenChange,
  canGrantAdmin,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  // ADMIN 역할은 ADMIN만 줄 수 있다
  canGrantAdmin: boolean
}) {
  const t = useTranslations("userAdmin")
  const fieldErrorText = useFieldErrorText()
  const errorText = useUserErrorText()
  const invalidate = useInvalidateUsers()
  const [formError, setFormError] = useState<string>()
  const [created, setCreated] = useState<{ loginId: string; value: string }>()
  const form = useForm<UserCreateInput>({
    resolver: zodResolver(userCreateSchema),
    defaultValues: { loginId: "", name: "", role: "USER" },
  })
  const finish = (next: boolean) => {
    if (!next) {
      setCreated(undefined)
      setFormError(undefined)
      form.reset()
    }
    onOpenChange(next)
  }
  const close = useDrawerClose(form.formState.isDirty && !created, finish)
  const create = useMutation({
    mutationFn: (values: UserCreateInput) => api.v1.userAdmin.create(values),
    onSuccess: async (result) => {
      await invalidate()
      setCreated({ loginId: result.user.loginId, value: result.temporaryPassword })
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("loginId", { message: "userAdmin.duplicate" })
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof UserCreateInput, { message: "" })
      setFormError(errorText(apiError))
    },
  })

  return (
    <SideDrawer
      open={open}
      onOpenChange={finish}
      title={t("create")}
      description={t("createDescription")}
      dirty={form.formState.isDirty && !created}
      footer={
        created ? (
          <Button onClick={() => finish(false)}>{t("done")}</Button>
        ) : (
          <>
            <Button type="button" variant="outline" onClick={() => void close()}>
              {t("cancel")}
            </Button>
            <Button type="submit" form="user-create-form" disabled={create.isPending}>
              {t("save")}
            </Button>
          </>
        )
      }
    >
      {created ? (
        <TemporaryPasswordPanel value={created.value} loginId={created.loginId} />
      ) : (
        <form
          id="user-create-form"
          noValidate
          onSubmit={form.handleSubmit((values) => {
            setFormError(undefined)
            create.mutate(values)
          })}
        >
          <FieldGroup>
            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            {(["loginId", "name"] as const).map((name) => (
              <Controller
                key={name}
                name={name}
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor={`user-${name}`}>{t(name)}</FieldLabel>
                    <Input
                      {...field}
                      id={`user-${name}`}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `user-${name}-error` : undefined}
                    />
                    {fieldState.invalid && (
                      <FieldError id={`user-${name}-error`} errors={fieldErrorText(fieldState.error)} />
                    )}
                  </Field>
                )}
              />
            ))}
            <Controller
              name="role"
              control={form.control}
              render={({ field }) => (
                <Field>
                  <FieldLabel htmlFor="user-role">{t("role")}</FieldLabel>
                  <NativeSelect id="user-role" value={field.value} onChange={field.onChange}>
                    {ROLES.filter((r) => r !== "ADMIN" || canGrantAdmin).map((r) => (
                      <NativeSelectOption key={r} value={r}>
                        {t(`roles.${r}`)}
                      </NativeSelectOption>
                    ))}
                  </NativeSelect>
                </Field>
              )}
            />
          </FieldGroup>
        </form>
      )}
    </SideDrawer>
  )
}
