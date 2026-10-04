"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { ErrorAlert } from "@/components/member/error-alert"
import { useSignUpFlow } from "@/components/member/sign-up-flow"
import { SignUpStep } from "@/components/member/sign-up-step"
import { useCountdown } from "@/components/member/use-countdown"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { errorCode } from "@/lib/api/client"
import { useRequestEmailVerification } from "@/lib/api/sign-up"

/** SCR-MEM-004 인증 메일 안내. 다시 보내기는 60초에 한 번 (INT-MAIL-002) */
export function SignUpEmailSent() {
  const t = useTranslations("signUp.sent")
  const { pendingEmail, setPendingEmail } = useSignUpFlow()
  const resend = useRequestEmailVerification()
  const [resent, setResent] = useState(false)
  const secondsLeft = useCountdown(pendingEmail?.resendAvailableAt)

  if (!pendingEmail) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>
            <h1>{t("title")}</h1>
          </CardTitle>
          <CardDescription>{t("noEmail")}</CardDescription>
        </CardHeader>
        <CardContent>
          <Link href="/signup/email" className={buttonVariants({ size: "lg", className: "w-full" })}>
            {t("changeEmail")}
          </Link>
        </CardContent>
      </Card>
    )
  }

  function send() {
    if (!pendingEmail) {
      return
    }
    setResent(false)
    resend.mutate(
      { email: pendingEmail.email, password: pendingEmail.password },
      {
        onSuccess: (sent) => {
          setResent(true)
          setPendingEmail({ ...pendingEmail, resendAvailableAt: sent.resendAvailableAt ?? new Date().toISOString() })
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
        <CardDescription>{t("body", { email: pendingEmail.email })}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <SignUpStep step="email" />
        {resend.error ? <ErrorAlert title={t("failedTitle")} code={errorCode(resend.error)} /> : null}
        {resent ? (
          <Alert>
            <AlertDescription>{t("resent")}</AlertDescription>
          </Alert>
        ) : null}
        <Button size="lg" variant="outline" disabled={secondsLeft > 0 || resend.isPending} onClick={send}>
          {secondsLeft > 0 ? t("resendIn", { seconds: secondsLeft }) : t("resend")}
        </Button>
        <p className="text-sm text-muted-foreground">{t("spam")}</p>
        <Link href="/signup/email" className={buttonVariants({ variant: "ghost" })}>
          {t("changeEmail")}
        </Link>
      </CardContent>
    </Card>
  )
}
