"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { Suspense, useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { OAuthErrorAlert } from "@/components/auth/oauth-error-alert"
import { fetchLandingPath } from "@/components/shell/landing"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel, FieldSeparator } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { currentNextPath } from "@/lib/next-path"
import { type SignInInput, signInSchema } from "@/lib/schemas/sign-in"
import { SITE } from "@/lib/site"
import { useAuthStore } from "@/stores/auth"

// moa: 화면에 단추를 둘 소셜 로그인 제공자 (specs/010-social-login FR-001). 디스코드는 나중에 더한다
const SOCIAL_PROVIDERS = ["google"] as const

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
  // moa: 설정된 제공자만 단추를 보인다 (US3-AC3). 목록을 못 받으면 단추 없이 기존 폼만
  const providers = useQuery({ queryKey: ["oauth", "providers"], queryFn: api.v1.oauth.providers })
  const social = SOCIAL_PROVIDERS.filter((provider) => providers.data?.providers.includes(provider))

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
      router.replace(await fetchLandingPath(queryClient))
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
            {/* moa: 소셜 로그인 실패 안내 (?oauthError=, specs/010-social-login US3) */}
            <Suspense fallback={null}>
              <OAuthErrorAlert />
            </Suspense>
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
            {social.length > 0 && <FieldSeparator>{t("or")}</FieldSeparator>}
            {social.map((provider) => (
              <Button
                key={provider}
                type="button"
                variant="outline"
                onClick={() => window.location.assign(api.v1.oauth.authorizePath(provider, currentNextPath()))}
              >
                {t(`continueWith.${provider}`)}
              </Button>
            ))}
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
