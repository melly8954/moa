"use client"

import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { ErrorAlert } from "@/components/member/error-alert"
import { SignUpExpired } from "@/components/member/sign-up-expired"
import { SignUpStep } from "@/components/member/sign-up-step"
import { useHydrated } from "@/components/member/use-hydrated"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { errorCode } from "@/lib/api/client"
import { useCancelSignUp, useCreateMember, useSignUp } from "@/lib/api/sign-up"
import { NICKNAME_MAX_LENGTH, NICKNAME_PATTERN } from "@/lib/validation/member-policy"

const SIGN_UP_REQUIRED = "A006"
const NICKNAME_TAKEN = "R003"

/** SCR-MEM-006 닉네임·필수 동의 (SEC-PRIV-001). 필수 동의를 모두 해야 가입 완료가 켜진다 */
export function SignUpProfileForm() {
  const t = useTranslations("signUp.profile")
  const router = useRouter()
  const signUp = useSignUp()
  const createMember = useCreateMember()
  const cancel = useCancelSignUp()
  const hydrated = useHydrated()

  const [nickname, setNickname] = useState("")
  const [nicknameError, setNicknameError] = useState<string | null>(null)
  const [termsAgreed, setTermsAgreed] = useState(false)
  const [privacyAgreed, setPrivacyAgreed] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)

  if (signUp.error && errorCode(signUp.error) === SIGN_UP_REQUIRED) {
    return <SignUpExpired />
  }

  const allAgreed = termsAgreed && privacyAgreed

  function agreeAll(checked: boolean) {
    setTermsAgreed(checked)
    setPrivacyAgreed(checked)
  }

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!NICKNAME_PATTERN.test(nickname)) {
      setNicknameError(t("invalidNickname"))
      return
    }
    setNicknameError(null)
    setFailure(null)
    createMember.mutate(
      { nickname, termsAgreed, privacyAgreed },
      {
        onSuccess: () => router.push("/"),
        onError: (error) => {
          setFailure(errorCode(error))
        },
      },
    )
  }

  function cancelSignUp() {
    cancel.mutate(undefined, { onSettled: () => router.push("/signup") })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <SignUpStep step="profile" />
        {failure && failure !== NICKNAME_TAKEN ? <ErrorAlert title={t("failedTitle")} code={failure} /> : null}
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <fieldset className="flex min-w-0 flex-col gap-4" disabled={!hydrated}>
            <div className="flex flex-col gap-2">
              <Label htmlFor="nickname">{t("nickname")}</Label>
              <Input
                id="nickname"
                autoComplete="nickname"
                maxLength={NICKNAME_MAX_LENGTH}
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                aria-invalid={Boolean(nicknameError) || failure === NICKNAME_TAKEN}
              />
              {nicknameError ? (
                <p className="text-sm text-destructive">{nicknameError}</p>
              ) : failure === NICKNAME_TAKEN ? (
                <NicknameTaken />
              ) : (
                <p className="text-sm text-muted-foreground">{t("nicknameHint")}</p>
              )}
            </div>
            <div className="flex flex-col gap-3">
              <ConsentCheckbox id="agree-all" checked={allAgreed} onChange={agreeAll} label={t("agreeAll")} />
              <Separator />
              <ConsentCheckbox id="agree-terms" checked={termsAgreed} onChange={setTermsAgreed} label={t("terms")} />
              <ConsentCheckbox
                id="agree-privacy"
                checked={privacyAgreed}
                onChange={setPrivacyAgreed}
                label={t("privacy")}
              />
              <ul className="flex flex-col gap-1 pl-6 text-sm text-muted-foreground">
                <li>{t("privacyEmail")}</li>
                <li>{t("privacyBirthDate")}</li>
                <li>{t("privacyPhone")}</li>
                <li>{t("privacyRetention")}</li>
              </ul>
            </div>
            <Button type="submit" size="lg" disabled={!allAgreed || createMember.isPending}>
              {t("submit")}
            </Button>
            <Button type="button" variant="ghost" onClick={cancelSignUp} disabled={cancel.isPending}>
              {t("cancel")}
            </Button>
          </fieldset>
        </form>
      </CardContent>
    </Card>
  )
}

function NicknameTaken() {
  const t = useTranslations("errors")
  return <p className="text-sm text-destructive">{t("R003")}</p>
}

function ConsentCheckbox({
  id,
  checked,
  onChange,
  label,
}: {
  id: string
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
}) {
  const labelId = `${id}-label`
  return (
    <div className="flex items-center gap-2">
      <Checkbox id={id} checked={checked} onCheckedChange={onChange} aria-labelledby={labelId} />
      <Label id={labelId} htmlFor={id}>
        {label}
      </Label>
    </div>
  )
}
