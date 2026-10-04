"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { ErrorAlert } from "@/components/member/error-alert"
import { useSignUpFlow } from "@/components/member/sign-up-flow"
import { SignUpStep } from "@/components/member/sign-up-step"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { errorCode } from "@/lib/api/client"
import { useRequestEmailVerification, useSignUp } from "@/lib/api/sign-up"
import { EMAIL_MAX_LENGTH, PASSWORD_PATTERN } from "@/lib/validation/member-policy"

const EMAIL_EXISTS = "B002"

/** SCR-MEM-003 이메일·비밀번호. 카카오가 이메일을 주지 않아 이어 온 가입이면 안내를 붙인다 */
export function SignUpEmailForm() {
  const t = useTranslations("signUp.email")
  const router = useRouter()
  const { setPendingEmail } = useSignUpFlow()
  const signUp = useSignUp()
  const request = useRequestEmailVerification()
  // 구글·카카오가 이메일을 주지 않았거나 인증되지 않은 이메일을 줘서 이 단계로 왔다 (제공자 이메일 없음)
  const providerWithoutEmail = Boolean(signUp.data?.provider) && !signUp.data?.email

  const schema = z
    .object({
      email: z
        .string()
        .trim()
        .max(EMAIL_MAX_LENGTH)
        .pipe(z.email(t("invalidEmail"))),
      password: z.string().regex(PASSWORD_PATTERN, t("invalidPassword")),
      passwordConfirm: z.string(),
    })
    .refine((values) => values.password === values.passwordConfirm, {
      message: t("passwordMismatch"),
      path: ["passwordConfirm"],
    })
  type Values = z.infer<typeof schema>
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "", passwordConfirm: "" },
  })
  const { errors } = form.formState

  const failure = request.error ? errorCode(request.error) : null
  const emailExists = failure === EMAIL_EXISTS

  function submit(values: Values) {
    request.mutate(
      { email: values.email, password: values.password },
      {
        onSuccess: (sent) => {
          setPendingEmail({
            email: sent.email ?? values.email,
            password: values.password,
            resendAvailableAt: sent.resendAvailableAt ?? new Date().toISOString(),
          })
          router.push("/signup/email/sent")
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <SignUpStep step="email" />
        {providerWithoutEmail ? (
          <Alert>
            <AlertTitle>{t("providerTitle")}</AlertTitle>
            <AlertDescription>{t("providerBody")}</AlertDescription>
          </Alert>
        ) : null}
        {emailExists ? (
          <Alert variant="destructive">
            <AlertTitle>{t("existingTitle")}</AlertTitle>
            <AlertDescription>{t("existingBody")}</AlertDescription>
          </Alert>
        ) : failure ? (
          <ErrorAlert title={t("failedTitle")} code={failure} />
        ) : null}
        <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(submit)} noValidate>
          <div className="flex flex-col gap-2">
            <Label htmlFor="email">{t("email")}</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              aria-invalid={Boolean(errors.email) || emailExists}
              {...form.register("email")}
            />
            {errors.email ? <p className="text-sm text-destructive">{errors.email.message}</p> : null}
            {emailExists ? <p className="text-sm text-destructive">{t("existingField")}</p> : null}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="password">{t("password")}</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              {...form.register("password")}
            />
            <p className={errors.password ? "text-sm text-destructive" : "text-sm text-muted-foreground"}>
              {errors.password ? errors.password.message : t("passwordHint")}
            </p>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="passwordConfirm">{t("passwordConfirm")}</Label>
            <Input
              id="passwordConfirm"
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.passwordConfirm)}
              {...form.register("passwordConfirm")}
            />
            {errors.passwordConfirm ? (
              <p className="text-sm text-destructive">{errors.passwordConfirm.message}</p>
            ) : null}
          </div>
          <Button type="submit" size="lg" disabled={request.isPending}>
            {t("submit")}
          </Button>
        </form>
        {emailExists ? (
          <div className="flex justify-center gap-2">
            <Link href="/login" className={buttonVariants({ variant: "outline" })}>
              {t("login")}
            </Link>
            <Link href="/password/forgot" className={buttonVariants({ variant: "outline" })}>
              {t("forgotPassword")}
            </Link>
          </div>
        ) : null}
        <Link href="/signup" className={buttonVariants({ variant: "ghost" })}>
          {t("back")}
        </Link>
      </CardContent>
    </Card>
  )
}
