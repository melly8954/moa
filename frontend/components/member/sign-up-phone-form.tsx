"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useCallback, useEffect, useRef, useState } from "react"
import { ErrorAlert } from "@/components/member/error-alert"
import { SignUpExpired } from "@/components/member/sign-up-expired"
import { SignUpStep } from "@/components/member/sign-up-step"
import { useHydrated } from "@/components/member/use-hydrated"
import { useCountdown } from "@/components/member/use-countdown"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { errorCode } from "@/lib/api/client"
import { useConfirmEmail, useConfirmPhone, useRequestPhoneCode, useSignUp } from "@/lib/api/sign-up"
import {
  ageOn,
  DAILY_SMS_LIMIT,
  EMAIL_LINK_TTL_MINUTES,
  MIN_SIGN_UP_AGE,
  PHONE_CODE_MAX_ATTEMPTS,
  PHONE_NUMBER_PATTERN,
  RESEND_INTERVAL_SECONDS,
  VERIFICATION_CODE_PATTERN,
} from "@/lib/validation/member-policy"

const SIGN_UP_REQUIRED = "A006"
const LINK_INVALID = "B004"
const SMS_SEND_FAILED = "S003"
const EMAIL_OWNER_SUSPENDED = "B016"
const EMAIL_OWNER_WITHDRAWN = "B017"
const CODE_MISMATCH = "B005"
const UNDER_AGE = "B010"
const PHONE_CONFLICTS = ["B011", "B012", "B013", "B014"]

/**
 * SCR-MEM-005 생년월일·휴대폰 인증. 이메일 인증 링크(?token=)로 열리면 먼저 링크를 확인한다.
 * 기존 계정을 찾으면 연결하고 로그인된 채로 홈으로 간다.
 */
export function SignUpPhoneForm({ token }: { token: string | null }) {
  const t = useTranslations("signUp.phone")
  const tCommon = useTranslations("common")
  const router = useRouter()
  const confirmEmail = useConfirmEmail()
  const startedToken = useRef<string | null>(null)
  const [emailConfirmed, setEmailConfirmed] = useState(token === null)

  const confirm = useCallback(
    (linkToken: string) => {
      confirmEmail.mutate(linkToken, {
        onSuccess: (result) => {
          if (result.result === "SIGNED_IN") {
            router.replace("/")
            return
          }
          setEmailConfirmed(true)
          router.replace("/signup/phone")
        },
      })
    },
    [confirmEmail, router],
  )

  useEffect(() => {
    if (!token || startedToken.current === token) {
      return
    }
    startedToken.current = token
    confirm(token)
  }, [token, confirm])

  function retry() {
    if (token) {
      confirm(token)
    }
  }

  if (confirmEmail.error) {
    const code = errorCode(confirmEmail.error)
    if (code === SIGN_UP_REQUIRED) {
      return <SignUpExpired />
    }
    return (
      <Card>
        <CardContent className="flex flex-col gap-4">
          {code === LINK_INVALID ? (
            <>
              <Alert variant="destructive">
                <AlertTitle>{t("linkInvalidTitle")}</AlertTitle>
                <AlertDescription>{t("linkInvalidBody", { minutes: EMAIL_LINK_TTL_MINUTES })}</AlertDescription>
              </Alert>
              <Link href="/signup/email" className={buttonVariants({ size: "lg" })}>
                {t("requestMailAgain")}
              </Link>
            </>
          ) : code === EMAIL_OWNER_SUSPENDED || code === EMAIL_OWNER_WITHDRAWN ? (
            <Alert variant="destructive">
              <AlertTitle>
                {code === EMAIL_OWNER_SUSPENDED ? t("emailSuspendedTitle") : t("emailWithdrawnTitle")}
              </AlertTitle>
              <AlertDescription>
                {code === EMAIL_OWNER_SUSPENDED ? t("emailSuspendedBody") : t("emailWithdrawnBody")}
              </AlertDescription>
            </Alert>
          ) : (
            <>
              <ErrorAlert title={t("linkFailedTitle")} code={code} />
              <Button size="lg" variant="outline" onClick={retry} disabled={confirmEmail.isPending}>
                {tCommon("retry")}
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    )
  }
  if (!emailConfirmed) {
    return (
      <Card aria-busy="true">
        <CardHeader>
          <CardDescription className="sr-only">{t("confirmingEmail")}</CardDescription>
          <Skeleton className="h-6 w-1/2" />
          <Skeleton className="h-4 w-full" />
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-9 w-full" />
        </CardContent>
      </Card>
    )
  }
  return <PhoneVerificationForm />
}

function PhoneVerificationForm() {
  const t = useTranslations("signUp.phone")
  const router = useRouter()
  const signUp = useSignUp()
  const requestCode = useRequestPhoneCode()
  const confirmPhone = useConfirmPhone()
  const hydrated = useHydrated()

  const [birthDate, setBirthDate] = useState("")
  const [phoneNumber, setPhoneNumber] = useState("")
  const [verificationCode, setVerificationCode] = useState("")
  const [fieldErrors, setFieldErrors] = useState<{ birthDate?: string; phoneNumber?: string; code?: string }>({})
  const [codeExpiresAt, setCodeExpiresAt] = useState<string | null>(null)
  const [resendAvailableAt, setResendAvailableAt] = useState<string | null>(null)
  const [failedAttempts, setFailedAttempts] = useState(0)
  const [failure, setFailure] = useState<string | null>(null)

  const resendSeconds = useCountdown(resendAvailableAt)
  const codeSeconds = useCountdown(codeExpiresAt)

  if (signUp.error && errorCode(signUp.error) === SIGN_UP_REQUIRED) {
    return <SignUpExpired />
  }

  function sendCode() {
    if (!PHONE_NUMBER_PATTERN.test(phoneNumber)) {
      setFieldErrors((current) => ({ ...current, phoneNumber: t("invalidPhone") }))
      return
    }
    setFieldErrors((current) => ({ ...current, phoneNumber: undefined }))
    setFailure(null)
    requestCode.mutate(phoneNumber, {
      onSuccess: (sent) => {
        setCodeExpiresAt(sent.expiresAt ?? null)
        setResendAvailableAt(sent.resendAvailableAt ?? null)
        setFailedAttempts(0)
      },
      onError: (error) => setFailure(errorCode(error)),
    })
  }

  function submit(event: React.FormEvent) {
    event.preventDefault()
    const errors: typeof fieldErrors = {}
    if (!birthDate) {
      errors.birthDate = t("invalidBirthDate")
    } else if (ageOn(birthDate) < MIN_SIGN_UP_AGE) {
      errors.birthDate = t("underAge")
    }
    if (!VERIFICATION_CODE_PATTERN.test(verificationCode)) {
      errors.code = t("invalidCode")
    }
    setFieldErrors(errors)
    if (errors.birthDate || errors.code) {
      return
    }
    setFailure(null)
    confirmPhone.mutate(
      { verificationCode, birthDate },
      {
        onSuccess: () => router.push("/signup/profile"),
        onError: (error) => {
          const code = errorCode(error)
          if (code === CODE_MISMATCH) {
            const failed = failedAttempts + 1
            setFailedAttempts(failed)
            setFieldErrors({ code: t("codeMismatch", { count: Math.max(0, PHONE_CODE_MAX_ATTEMPTS - failed) }) })
            return
          }
          if (code === UNDER_AGE) {
            setFieldErrors({ birthDate: t("underAge") })
          }
          setFailure(code)
        },
      },
    )
  }

  const codeRequested = codeExpiresAt !== null
  const minutes = Math.floor(codeSeconds / 60)
  const seconds = String(codeSeconds % 60).padStart(2, "0")

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
        <CardDescription>{t("description")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <SignUpStep step="phone" />
        {failure ? <FailureAlert code={failure} maskedEmail={signUp.data?.phoneConflict?.maskedEmail} /> : null}
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <fieldset className="flex min-w-0 flex-col gap-4" disabled={!hydrated}>
            <div className="flex flex-col gap-2">
              <Label htmlFor="birthDate">{t("birthDate")}</Label>
              <Input
                id="birthDate"
                type="date"
                value={birthDate}
                onChange={(event) => setBirthDate(event.target.value)}
                aria-invalid={Boolean(fieldErrors.birthDate)}
              />
              {fieldErrors.birthDate ? <p className="text-sm text-destructive">{fieldErrors.birthDate}</p> : null}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="phoneNumber">{t("phone")}</Label>
              <Input
                id="phoneNumber"
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                placeholder={t("phonePlaceholder")}
                value={phoneNumber}
                onChange={(event) => setPhoneNumber(event.target.value.replace(/\D/g, ""))}
                aria-invalid={Boolean(fieldErrors.phoneNumber)}
              />
              {fieldErrors.phoneNumber ? <p className="text-sm text-destructive">{fieldErrors.phoneNumber}</p> : null}
              <Button
                type="button"
                variant="outline"
                onClick={sendCode}
                disabled={resendSeconds > 0 || requestCode.isPending}
              >
                {!codeRequested
                  ? t("requestCode")
                  : resendSeconds > 0
                    ? t("resendCodeIn", { seconds: resendSeconds })
                    : t("resendCode")}
              </Button>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="verificationCode">{t("code")}</Label>
              <Input
                id="verificationCode"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={verificationCode}
                onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, ""))}
                aria-invalid={Boolean(fieldErrors.code)}
              />
              {fieldErrors.code ? (
                <p className="text-sm text-destructive">{fieldErrors.code}</p>
              ) : (
                <p className="text-sm text-muted-foreground">
                  {codeRequested
                    ? t("codeHint", { time: `${minutes}:${seconds}`, attempts: PHONE_CODE_MAX_ATTEMPTS })
                    : t("codeFirst")}
                </p>
              )}
            </div>
            <Button type="submit" size="lg" disabled={confirmPhone.isPending}>
              {t("next")}
            </Button>
          </fieldset>
        </form>
      </CardContent>
    </Card>
  )
}

/** 휴대폰 인증 실패 안내. 번호가 겹치면 회원 정책의 휴대폰 번호 중복 안내를 따른다 */
function FailureAlert({ code, maskedEmail }: { code: string; maskedEmail?: string | null }) {
  const t = useTranslations("signUp.phone")
  if (code === SMS_SEND_FAILED) {
    return (
      <Alert variant="destructive">
        <AlertTitle>{t("smsFailedTitle")}</AlertTitle>
        <AlertDescription>
          {t("smsFailedBody", { seconds: RESEND_INTERVAL_SECONDS, limit: DAILY_SMS_LIMIT })}
        </AlertDescription>
      </Alert>
    )
  }
  if (!PHONE_CONFLICTS.includes(code)) {
    const title = code === "B008" || code === "B009" ? t("smsFailedTitle") : undefined
    return <ErrorAlert title={title} code={code} />
  }
  if (code === "B011" || code === "B012") {
    const active = code === "B011"
    return (
      <Alert variant="destructive">
        <AlertTitle>{active ? t("activeTitle") : t("suspendedTitle")}</AlertTitle>
        {maskedEmail ? (
          <AlertDescription>
            {active ? t("activeBody", { email: maskedEmail }) : t("suspendedBody", { email: maskedEmail })}
          </AlertDescription>
        ) : null}
      </Alert>
    )
  }
  const withdrawn = code === "B013"
  return (
    <Alert variant="destructive">
      <AlertTitle>{withdrawn ? t("withdrawnTitle") : t("restrictedTitle")}</AlertTitle>
      <AlertDescription>{withdrawn ? t("withdrawnBody") : t("restrictedBody")}</AlertDescription>
    </Alert>
  )
}
