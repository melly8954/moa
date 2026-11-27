"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { SegmentedButtons } from "@/components/kit/segmented-buttons"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { StatusText } from "@/components/kit/status-text"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { useFieldErrorText } from "@/i18n/field-error"
import { formatDateTime } from "@/lib/format"
import { type UserCreateInput, type UserEditInput, userCreateSchema, userEditSchema } from "@/lib/schemas/user-admin"
import type { AdminUser } from "@/types/user-admin"
import { ROLES, TemporaryPasswordPanel, useInvalidateUsers, useUserErrorText } from "./user-dialogs"
import type { UserAction } from "./user-extras"
import { UserRoles, useGivenRoles } from "./user-roles"
import { loginMethodText, passwordHint } from "./user-social"

// 사용자 드로어 (moa, specs/002-admin-users FR-007, US3·US4, research R7·R11·R16). 목록에서 행을 누르면 열린다
// 2026-11-22: 세 구역으로 나눈다 (사용자가 미리보기 세 안 중 A를 골랐다)
//   계정 정보 (한 폼): 아이디·상태·만든 시각은 보기만, 이름 입력란, 계정 종류 나눔 단추, 역할 체크 칸(UserRoles)
//     드로어 아래 저장으로 바뀐 것만 차례로 보낸다: 이름 → 계정 종류(바뀌면 확인 창) → 역할. 바꾼 채 닫으면 묻는다
//   계정 상태: 로그인 막기 스위치(= 잠그기·풀기), 비밀번호 초기화. 확인 창을 거쳐 바로 적용한다 (폼 저장과 따로)
//   계정 삭제: 맨 아래 빨간 테두리 구역
//   다룰 수 없는 계정(protection: 최고 관리자 계정, 나보다 권한이 많은 관리자 계정)은 칸·스위치·단추를 모두 끄고 이유를 보인다
//   내 행(계정 종류·로그인 막기·삭제), 비밀번호 없는 계정(초기화)도 끈다. 서버도 같은 규칙으로 막는다
//   계정을 바꾸는 일은 한 사람씩 확인하므로 일괄 처리는 두지 않는다
export function UserDrawer({
  user,
  onClose,
  isSelf,
  extraActions,
  onToggleLock,
  onResetPassword,
  onRemove,
}: {
  user?: AdminUser
  onClose: () => void
  isSelf: boolean
  extraActions: UserAction[]
  onToggleLock: () => void
  onResetPassword: () => void
  onRemove: () => void
}) {
  const t = useTranslations("userAdmin")
  const fieldErrorText = useFieldErrorText()
  const errorText = useUserErrorText()
  const queryClient = useQueryClient()
  const protectedRow = !!user?.protection
  // moa: 최고 관리자 계정은 본인 이름 바꾸기만 된다. 나머지는 누구도 못 한다 (FR-007, 2026-11-21 한 명 고정)
  const fixedRow = !!user?.superAdmin
  const given = useGivenRoles(user)
  const form = useForm<UserEditInput>({
    resolver: zodResolver(userEditSchema),
    // 목록·받은 역할을 다시 읽으면 맞춘다. 고치던 칸은 그대로 둔다
    values: user ? { name: user.name, role: user.role, roleIds: given.ids } : undefined,
    resetOptions: { keepDirtyValues: true },
  })
  const dirty = form.formState.isDirty
  const accountType = useWatch({ control: form.control, name: "role" })
  const close = useDrawerClose(dirty, (open) => !open && onClose())

  const save = useMutation({
    mutationFn: async (values: UserEditInput) => {
      if (!user) return
      if (values.name !== user.name) await api.v1.userAdmin.rename(user.id, values.name)
      if (values.role !== user.role) await api.v1.userAdmin.changeRole(user.id, values.role)
      const before = user.role === "ADMIN" ? given.ids : []
      const rolesChanged = values.roleIds.length !== before.length || values.roleIds.some((id) => !before.includes(id))
      if (values.role === "ADMIN" && given.canManage && !user.superAdmin && rolesChanged)
        await api.v1.role.replaceUserRoles(user.id, values.roleIds)
    },
    onSuccess: () => toast.success(t("saved")),
    onError: (error) => {
      const apiError = toApiError(error)
      // 역할 규칙(B001)은 서버 문구가 더 자세하다
      toast.error(apiError.code === "B001" && apiError.message ? apiError.message : errorText(error))
    },
    // 중간에 실패해도 된 것은 남으므로 다시 읽어 폼을 맞춘다
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "users"] })
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
    },
  })

  const submit = form.handleSubmit(async (values) => {
    if (!user) return
    if (values.role !== user.role) {
      const demote = user.role === "ADMIN" && user.accessRoles.length > 0
      const ok = await confirm({
        title: t("changeRoleTitle", { loginId: user.loginId, role: t(`roles.${values.role}`) }),
        description: demote
          ? t("demoteWarning", { roles: user.accessRoles.map((r) => r.name).join(", ") })
          : t("changeRoleDescription"),
        destructive: demote,
      })
      if (!ok) return
    }
    save.mutate(values)
  })

  return (
    <SideDrawer
      open={!!user}
      onOpenChange={(open) => !open && onClose()}
      title={user?.loginId ?? ""}
      description={t("drawerDescription")}
      dirty={dirty}
      footer={
        user && (
          <>
            <Button type="button" variant="outline" onClick={() => void close()}>
              {t("cancel")}
            </Button>
            <Button type="submit" form="user-edit-form" disabled={!dirty || save.isPending}>
              {t("save")}
            </Button>
          </>
        )
      }
    >
      {user && (
        <>
          {user.protection ? (
            <Alert>
              <AlertDescription>
                {t(user.protection === "SUPER_ADMIN_ONLY" ? "protectedSuperAdmin" : "protectedWider")}
              </AlertDescription>
            </Alert>
          ) : (
            fixedRow && (
              <Alert>
                <AlertDescription>{t("superAdminSelf")}</AlertDescription>
              </Alert>
            )
          )}
          <form id="user-edit-form" noValidate onSubmit={(e) => void submit(e)} className="flex flex-col gap-4">
            <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 text-sm">
              <dt className="text-muted-foreground">{t("loginId")}</dt>
              <dd>{user.loginId}</dd>
              <dt className="text-muted-foreground">{t("loginMethod")}</dt>
              <dd>{loginMethodText(user, t)}</dd>
              <dt className="text-muted-foreground">{t("status")}</dt>
              <dd>
                <UserStatus user={user} />
              </dd>
              <dt className="text-muted-foreground">{t("createdAt")}</dt>
              <dd>{formatDateTime(user.createdAt)}</dd>
            </dl>
            <FieldGroup>
              <Controller
                name="name"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="user-edit-name">{t("name")}</FieldLabel>
                    <Input
                      {...field}
                      id="user-edit-name"
                      disabled={protectedRow || save.isPending}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? "user-edit-name-error" : undefined}
                    />
                    {fieldState.invalid && (
                      <FieldError id="user-edit-name-error" errors={fieldErrorText(fieldState.error)} />
                    )}
                  </Field>
                )}
              />
              <Controller
                name="role"
                control={form.control}
                render={({ field }) => (
                  <Field>
                    <FieldLabel id="user-edit-role-label">{t("role")}</FieldLabel>
                    <div>
                      <SegmentedButtons
                        aria-labelledby="user-edit-role-label"
                        items={ROLES.map((r) => ({ value: r, label: t(`roles.${r}`) }))}
                        value={field.value}
                        onChange={field.onChange}
                        disabled={protectedRow || fixedRow || isSelf || save.isPending}
                      />
                    </div>
                  </Field>
                )}
              />
            </FieldGroup>
            <section aria-labelledby="user-edit-roles" className="flex flex-col gap-2 border-t pt-4">
              <h3 id="user-edit-roles" className="text-sm font-medium">
                {t("accessRoles")}
              </h3>
              {given.isPending ? (
                <Skeleton className="h-24 w-full" />
              ) : (
                <Controller
                  name="roleIds"
                  control={form.control}
                  render={({ field }) => (
                    <UserRoles
                      user={user}
                      accountType={accountType}
                      value={field.value}
                      onChange={field.onChange}
                      disabled={protectedRow || save.isPending}
                    />
                  )}
                />
              )}
            </section>
          </form>
          <section aria-labelledby="user-status-section" className="flex flex-col gap-4 border-t pt-4">
            <h3 id="user-status-section" className="text-sm font-medium">
              {t("statusSection")}
            </h3>
            <SettingRow label={t("loginBlock")} hint={t("loginBlockHint")} htmlFor="user-login-block">
              <Switch
                id="user-login-block"
                checked={user.locked}
                disabled={protectedRow || fixedRow || isSelf}
                onCheckedChange={() => onToggleLock()}
              />
            </SettingRow>
            <SettingRow label={t("password")} hint={passwordHint(user, t)}>
              <Button
                size="sm"
                variant="outline"
                disabled={protectedRow || fixedRow || !user.hasPassword}
                onClick={onResetPassword}
              >
                {t("reset")}
              </Button>
            </SettingRow>
            {extraActions.map((action) => (
              <SettingRow key={action.key} label={action.label}>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={protectedRow || fixedRow || action.disabled?.(user)}
                  onClick={() => action.onSelect(user)}
                >
                  {action.label}
                </Button>
              </SettingRow>
            ))}
          </section>
          <section
            aria-labelledby="user-remove-section"
            className="flex items-center justify-between gap-4 rounded-lg border border-destructive/40 p-3"
          >
            <div className="flex flex-col gap-0.5">
              <h3 id="user-remove-section" className="text-sm font-medium text-destructive">
                {t("removeSection")}
              </h3>
              <p className="text-xs text-muted-foreground">{t("removeDescription")}</p>
            </div>
            <Button size="sm" variant="destructive" disabled={protectedRow || fixedRow || isSelf} onClick={onRemove}>
              {t("remove")}
            </Button>
          </section>
        </>
      )}
    </SideDrawer>
  )
}

// 계정 상태 한 줄: 왼쪽 이름·설명, 오른쪽 스위치나 단추
function SettingRow({
  label,
  hint,
  htmlFor,
  children,
}: {
  label: string
  hint?: string
  htmlFor?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex min-w-0 flex-col gap-0.5">
        {htmlFor ? (
          <label htmlFor={htmlFor} className="text-sm">
            {label}
          </label>
        ) : (
          <span className="text-sm">{label}</span>
        )}
        {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
      </div>
      {children}
    </div>
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
// moa: 계정 종류(회원·관리자) 모두 사용자 관리 권한으로 만든다 (specs/002-admin-users FR-005). 역할은 만든 뒤 드로어에서 준다
export function CreateUserDrawer({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
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
                    {ROLES.map((r) => (
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
