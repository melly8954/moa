"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Copy } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type UserRenameInput, userRenameSchema } from "@/lib/schemas/user-admin"
import type { Role } from "@/types/user"
import type { AdminUser } from "@/types/user-admin"

// 사용자 관리 화면의 창들: 이름 바꾸기, 역할 바꾸기, 임시 비밀번호 보이기 (만들기는 user-drawer.tsx의 드로어)

export const ROLES: Role[] = ["USER", "ADMIN"]

export function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] })
}

// 서버 오류 문구. A005(ADMIN 보호)·B001(자기 자신)은 사용자 관리에 맞는 문구로
export function useUserErrorText() {
  const t = useTranslations("userAdmin")
  const { describeError } = useErrorMessages()
  return (error: unknown) => {
    const apiError = toApiError(error)
    if (apiError.code === "A005") return t("adminOnly")
    if (apiError.code === "B001") return t("notAllowed")
    return describeError(apiError).message
  }
}

// 임시 비밀번호: 이 창(또는 만들기 드로어)에서만 보인다. 복사 버튼을 둔다
export function TemporaryPasswordPanel({ value, loginId }: { value: string; loginId: string }) {
  const t = useTranslations("userAdmin")
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      toast.success(t("copied"))
    } catch {
      toast.error(t("copyFailed"))
    }
  }
  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">{t("temporaryPasswordDescription", { loginId })}</p>
      <div className="flex items-center gap-2">
        <Input readOnly value={value} aria-label={t("temporaryPassword")} className="font-mono" />
        <Button variant="outline" onClick={() => void copy()}>
          <Copy />
          {t("copy")}
        </Button>
      </div>
      <Alert>
        <AlertDescription>{t("temporaryPasswordOnce")}</AlertDescription>
      </Alert>
    </div>
  )
}

export function TemporaryPasswordDialog({
  value,
  loginId,
  onClose,
}: {
  value?: string
  loginId?: string
  onClose: () => void
}) {
  const t = useTranslations("userAdmin")
  return (
    <Dialog open={!!value} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("temporaryPasswordTitle")}</DialogTitle>
          <DialogDescription className="sr-only">{t("temporaryPasswordTitle")}</DialogDescription>
        </DialogHeader>
        <TemporaryPasswordPanel value={value ?? ""} loginId={loginId ?? ""} />
        <DialogFooter>
          <Button onClick={onClose}>{t("done")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function RenameDialog({ user, onClose }: { user?: AdminUser; onClose: () => void }) {
  const t = useTranslations("userAdmin")
  const fieldErrorText = useFieldErrorText()
  const errorText = useUserErrorText()
  const invalidate = useInvalidateUsers()
  const [formError, setFormError] = useState<string>()
  const form = useForm<UserRenameInput>({ resolver: zodResolver(userRenameSchema), values: { name: user?.name ?? "" } })
  const rename = useMutation({
    mutationFn: ({ name }: UserRenameInput) => api.v1.userAdmin.rename(user!.id, name),
    onSuccess: async () => {
      await invalidate()
      toast.success(t("renamed"))
      onClose()
    },
    onError: (error) => setFormError(errorText(error)),
  })
  return (
    <Dialog open={!!user} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("rename")}</DialogTitle>
          <DialogDescription>{user?.loginId}</DialogDescription>
        </DialogHeader>
        <form
          noValidate
          onSubmit={form.handleSubmit((values) => {
            setFormError(undefined)
            rename.mutate(values)
          })}
        >
          <FieldGroup>
            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            <Controller
              name="name"
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="rename-name">{t("name")}</FieldLabel>
                  <Input
                    {...field}
                    id="rename-name"
                    aria-invalid={fieldState.invalid}
                    aria-describedby={fieldState.invalid ? "rename-name-error" : undefined}
                  />
                  {fieldState.invalid && (
                    <FieldError id="rename-name-error" errors={fieldErrorText(fieldState.error)} />
                  )}
                </Field>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={rename.isPending}>
                {t("save")}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function RoleChangeDialog({ user, onClose }: { user?: AdminUser; onClose: () => void }) {
  const t = useTranslations("userAdmin")
  const errorText = useUserErrorText()
  const invalidate = useInvalidateUsers()
  const [role, setRole] = useState<Role>(user?.role ?? "USER")
  const [lastUser, setLastUser] = useState(user)
  if (user !== lastUser) {
    setLastUser(user)
    setRole(user?.role ?? "USER")
  }
  const change = useMutation({
    mutationFn: () => api.v1.userAdmin.changeRole(user!.id, role),
    onSuccess: async () => {
      await invalidate()
      toast.success(t("roleChanged"))
      onClose()
    },
    onError: (error) => toast.error(errorText(error)),
  })
  return (
    <Dialog open={!!user} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("changeRole")}</DialogTitle>
          <DialogDescription>{t("changeRoleDescription")}</DialogDescription>
        </DialogHeader>
        <Field>
          <FieldLabel htmlFor="change-role">{t("role")}</FieldLabel>
          <NativeSelect id="change-role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {ROLES.map((r) => (
              <NativeSelectOption key={r} value={r}>
                {t(`roles.${r}`)}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t("cancel")}
          </Button>
          <Button disabled={change.isPending || role === user?.role} onClick={() => change.mutate()}>
            {t("save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
