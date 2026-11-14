"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { landingPath } from "@/components/shell/landing"
import { MY_MENUS_QUERY_KEY } from "@/components/shell/use-menu"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { currentNextPath } from "@/lib/next-path"
import { type SignInInput, signInSchema } from "@/lib/schemas/sign-in"
import { SITE } from "@/lib/site"
import { useAuthStore } from "@/stores/auth"

// 폼 만드는 모양의 예시이기도 하다: Zod 스키마 → react-hook-form → shadcn Field → api 호출 → 오류 표시
export default function SignInPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const t = useTranslations("signIn")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const form = useForm<SignInInput>({
    resolver: zodResolver(signInSchema),
    defaultValues: { loginId: "", password: "" },
  })

  const onSubmit = async (values: SignInInput) => {
    setFormError(undefined)
    try {
      const token = await api.v1.auth.login(values)
      queryClient.clear()
      useAuthStore.getState().signIn(token.accessToken)
      // 로그인이 필요한 화면에서 왔으면 그 화면으로 돌아간다 (?next=, 같은 사이트 경로만)
      // 아니면 관리 메뉴가 있으면 관리 셸, 없으면 사용자 셸 첫 화면 (moa: specs/001-admin-menus FR-006)
      const next = currentNextPath()
      if (next) {
        router.replace(next)
        return
      }
      // 메뉴를 읽지 못하면 사용자 셸 첫 화면으로 간다 (권한은 화면마다 API가 막는다)
      const menus = await queryClient
        .fetchQuery({ queryKey: MY_MENUS_QUERY_KEY, queryFn: api.v1.menu.mine })
        .catch(() => [])
      router.replace(landingPath(menus))
    } catch (error) {
      const apiError = toApiError(error)
      // 서버 V001: 해당 칸만 오류로 표시하고 공통 문구를 보인다
      for (const fieldError of apiError.fieldErrors) {
        if (fieldError.field in values) form.setError(fieldError.field as keyof SignInInput, { message: "" })
      }
      setFormError(describeError(apiError).message)
    }
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>
          <h1>{t("title", { name: SITE.name })}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <FieldGroup>
            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            <Controller
              name="loginId"
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="loginId">{t("loginId")}</FieldLabel>
                  <Input
                    {...field}
                    id="loginId"
                    autoComplete="username"
                    aria-invalid={fieldState.invalid}
                    aria-describedby={fieldState.invalid ? "loginId-error" : undefined}
                  />
                  {fieldState.invalid && <FieldError id="loginId-error" errors={fieldErrorText(fieldState.error)} />}
                </Field>
              )}
            />
            <Controller
              name="password"
              control={form.control}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="password">{t("password")}</FieldLabel>
                  <Input
                    {...field}
                    id="password"
                    type="password"
                    autoComplete="current-password"
                    aria-invalid={fieldState.invalid}
                    aria-describedby={fieldState.invalid ? "password-error" : undefined}
                  />
                  {fieldState.invalid && <FieldError id="password-error" errors={fieldErrorText(fieldState.error)} />}
                </Field>
              )}
            />
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {t("submit")}
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
