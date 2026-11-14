"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { useUnsavedChanges } from "@/components/kit/use-unsaved-changes"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { ME_QUERY_KEY, useMe } from "@/lib/me"
import { currentNextPath } from "@/lib/next-path"
import { type PasswordChangeInput, passwordChangeSchema } from "@/lib/schemas/password"

const FIELDS = [
  { name: "currentPassword", autoComplete: "current-password" },
  { name: "newPassword", autoComplete: "new-password", hint: true },
  { name: "newPasswordConfirm", autoComplete: "new-password" },
] as const

// 내 비밀번호 바꾸기. 임시 비밀번호를 받은 사용자는 AuthGate가 여기로 보내고, 바꾸기 전에는 다른 화면으로 가지 못한다
export default function PasswordPage() {
  const t = useTranslations("password")
  const router = useRouter()
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const required = useMe().data?.passwordChangeRequired === true
  const [formError, setFormError] = useState<string>()
  const form = useForm<PasswordChangeInput>({
    resolver: zodResolver(passwordChangeSchema),
    defaultValues: { currentPassword: "", newPassword: "", newPasswordConfirm: "" },
  })
  const { confirmLeave } = useUnsavedChanges(form.formState.isDirty)

  const onSubmit = async ({ currentPassword, newPassword }: PasswordChangeInput) => {
    setFormError(undefined)
    try {
      await api.v1.user.changePassword({ currentPassword, newPassword })
      form.reset()
      await queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY })
      toast.success(t("done"))
      router.replace(currentNextPath() ?? "/")
    } catch (error) {
      const apiError = toApiError(error)
      // 서버 V001에 칸 오류가 없으면 지금 비밀번호가 틀린 경우다 (새 비밀번호가 같은 경우는 화면이 먼저 막는다)
      if (apiError.code === "V001" && apiError.fieldErrors.length === 0) {
        form.setError("currentPassword", { message: "password.currentWrong" })
        return
      }
      for (const fieldError of apiError.fieldErrors) {
        if (FIELDS.some((f) => f.name === fieldError.field))
          form.setError(fieldError.field as keyof PasswordChangeInput, { message: "" })
      }
      setFormError(describeError(apiError).message)
    }
  }

  const cancel = async () => {
    if (await confirmLeave()) router.push("/")
  }

  return (
    <Card className="w-full max-w-(--form-max-width)">
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <FieldGroup>
            {required && (
              <Alert>
                <AlertDescription>{t("required")}</AlertDescription>
              </Alert>
            )}
            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            {FIELDS.map((f) => (
              <Controller
                key={f.name}
                name={f.name}
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor={f.name}>{t(f.name)}</FieldLabel>
                    <Input
                      {...field}
                      id={f.name}
                      type="password"
                      autoComplete={f.autoComplete}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `${f.name}-error` : undefined}
                    />
                    {"hint" in f && <FieldDescription>{t("newPasswordHint")}</FieldDescription>}
                    {fieldState.invalid && (
                      <FieldError id={`${f.name}-error`} errors={fieldErrorText(fieldState.error)} />
                    )}
                  </Field>
                )}
              />
            ))}
            <div className="flex justify-end gap-2">
              {!required && (
                <Button type="button" variant="outline" onClick={() => void cancel()}>
                  {t("cancel")}
                </Button>
              )}
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {t("submit")}
              </Button>
            </div>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
