"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { LegalDocument } from "@/components/legal/legal-document"
import { LegalFooter } from "@/components/legal/legal-footer"
import { fetchLandingPath } from "@/components/shell/landing"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { termsText } from "@/content/terms"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { currentNextPath } from "@/lib/next-path"
import { type SignUpInput, signUpSchema } from "@/lib/schemas/sign-up"
import { useAuthStore } from "@/stores/auth"
import type { TermsType } from "@/types/oauth"

// moa: 소셜 가입 닉네임·약관 화면 (specs/010-social-login FR-005·FR-013, US1-AC1·AC3·AC7·AC13·AC14)
// 콜백이 가입 대기 쿠키(oauth_signup)를 심고 여기로 보낸다. 가입을 마치기 전에는 회원이 없다
// 메뉴로 여는 화면이 아니라 프로그램으로 등록하지 않는다 (FR-005)
// 약관 칸: 전문을 높이가 정해진 상자에 바로 보이고, 아래 링크로 문서 화면을 새 탭에서 연다 (FR-013, specs/012-terms-privacy FR-005)
//   이용약관 전문은 공개 문서 API(가입 동의 버전), 개인정보 수집·이용 동의는 프론트 문안(content/terms)
const TERMS_FIELDS: { type: TermsType; field: "agreeService" | "agreePrivacy" }[] = [
  { type: "SERVICE", field: "agreeService" },
  { type: "PRIVACY", field: "agreePrivacy" },
]

// 로그인 화면으로 다시 가야 하는 오류 (가입 대기가 끝났거나 이미 가입됨, 가입 확인에 막힘)
const RESTART_CODES = new Set(["A009", "A012", "A014", "A015"])

export default function SignUpNicknamePage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const t = useTranslations("signUp")
  const tLegal = useTranslations("legal")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const [mustRestart, setMustRestart] = useState(false)
  const terms = useQuery({ queryKey: ["terms", "current"], queryFn: api.v1.terms.current })
  const form = useForm<SignUpInput>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { nickname: "", agreeService: false, agreePrivacy: false },
  })

  const versionOf = (type: TermsType) => terms.data?.terms.find((item) => item.type === type)?.version
  const serviceVersion = versionOf("SERVICE")
  const service = useQuery({
    queryKey: ["legal", "terms", "versions", serviceVersion],
    queryFn: () => api.v1.legal.version("terms", serviceVersion ?? ""),
    enabled: serviceVersion !== undefined,
  })
  const privacyVersion = versionOf("PRIVACY")
  const privacyText = privacyVersion === undefined ? undefined : termsText("PRIVACY", privacyVersion)
  const termsReady = service.data !== undefined && privacyText !== undefined
  const [agreeService, agreePrivacy] = useWatch({ control: form.control, name: ["agreeService", "agreePrivacy"] })

  const onSubmit = async (values: SignUpInput) => {
    setFormError(undefined)
    try {
      const token = await api.v1.oauth.signUp({
        nickname: values.nickname,
        agreements: TERMS_FIELDS.map(({ type }) => ({ type, version: versionOf(type) ?? "" })),
      })
      queryClient.clear()
      useAuthStore.getState().signIn(token.accessToken)
      // 로그인이 필요한 화면에서 왔으면 그 화면으로 (US1-AC4), 아니면 첫 화면 규칙 (001-admin-menus FR-006)
      const next = currentNextPath()
      router.replace(next ?? (await fetchLandingPath(queryClient)))
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.code === "V001" && apiError.fieldErrors.some((e) => e.field === "nickname")) {
        form.setError("nickname", { message: "signUp.nicknameInvalid" })
        return
      }
      if (apiError.code === "B201") {
        form.setError("nickname", { message: "signUp.nicknameTagsExhausted" })
        return
      }
      if (apiError.code === "A017") {
        // 화면을 연 사이 약관 버전이 바뀌었다. 새 문안으로 다시 동의받는다
        form.setValue("agreeService", false)
        form.setValue("agreePrivacy", false)
        await terms.refetch()
      }
      setMustRestart(RESTART_CODES.has(apiError.code))
      setFormError(describeError(apiError).message)
    }
  }

  return (
    <>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>
            <h1>{t("title")}</h1>
          </CardTitle>
          <CardDescription>{t("description")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
            <FieldGroup>
              {formError && (
                <Alert variant="destructive">
                  <AlertDescription>
                    {formError}
                    {mustRestart && (
                      <Link href="/sign-in" className="ml-1 underline">
                        {t("backToSignIn")}
                      </Link>
                    )}
                  </AlertDescription>
                </Alert>
              )}
              {(terms.isError || service.isError) && (
                <Alert variant="destructive">
                  <AlertDescription>{t("termsLoadFailed")}</AlertDescription>
                </Alert>
              )}
              <Controller
                name="nickname"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="nickname">{t("nickname")}</FieldLabel>
                    <Input
                      {...field}
                      id="nickname"
                      autoComplete="nickname"
                      maxLength={12}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? "nickname-error" : "nickname-help"}
                    />
                    <FieldDescription id="nickname-help">{t("nicknameHelp")}</FieldDescription>
                    {fieldState.invalid && <FieldError id="nickname-error" errors={fieldErrorText(fieldState.error)} />}
                  </Field>
                )}
              />
              <FieldSet>
                {TERMS_FIELDS.map(({ type, field: name }) => {
                  const ready = type === "SERVICE" ? service.data !== undefined : privacyText !== undefined
                  return (
                    <Controller
                      key={type}
                      name={name}
                      control={form.control}
                      render={({ field, fieldState }) => (
                        <Field orientation="horizontal" data-invalid={fieldState.invalid}>
                          <Checkbox
                            id={name}
                            checked={field.value}
                            onCheckedChange={(checked) => field.onChange(checked === true)}
                            disabled={!ready}
                            aria-invalid={fieldState.invalid}
                          />
                          <FieldContent className="min-w-0">
                            <FieldLabel htmlFor={name}>{t(`terms.${type}`)}</FieldLabel>
                            {type === "SERVICE" && service.data && (
                              <>
                                <LegalDocument content={service.data} compact label={t(`terms.${type}`)} />
                                <LegalLink href="/terms">{tLegal("more")}</LegalLink>
                              </>
                            )}
                            {type === "PRIVACY" && privacyText && (
                              <>
                                <p
                                  role="region"
                                  aria-label={t(`terms.${type}`)}
                                  tabIndex={0}
                                  className="max-h-48 overflow-y-auto rounded-md border bg-muted/30 p-3 text-sm whitespace-pre-line"
                                >
                                  {privacyText.body}
                                </p>
                                <LegalLink href="/privacy">{tLegal("viewPrivacy")}</LegalLink>
                              </>
                            )}
                          </FieldContent>
                        </Field>
                      )}
                    />
                  )
                })}
              </FieldSet>
              <Button
                type="submit"
                disabled={!termsReady || !agreeService || !agreePrivacy || form.formState.isSubmitting}
              >
                {t("submit")}
              </Button>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
      {/* 맨 아래 푸터 줄. 쓰던 내용을 잃지 않게 새 탭에서 연다 (012 US2-AC4) */}
      <LegalFooter newTab className="mt-4" />
    </>
  )
}

// 약관 칸 아래 링크. 새 탭에서 연다 (specs/012-terms-privacy FR-005)
function LegalLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link href={href} target="_blank" rel="noopener" className="w-fit text-sm font-medium underline underline-offset-4">
      {children}
    </Link>
  )
}
