"use client"

import { useTranslations } from "next-intl"
import { useState } from "react"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useErrorMessages } from "@/lib/error-messages"
import { withdrawnSignInPath } from "@/lib/next-path"
import { unsubscribeBrowserOnly } from "@/lib/web-push"

// moa: 탈퇴 확인 창 (specs/018-member-withdrawal contracts/screens.md "탈퇴 확인 창", US1-AC2·AC3·AC9)
//   안내 세 가지, 닉네임 입력. 앞뒤 공백을 뺀 값이 지금 닉네임과 같아야 "탈퇴하기"가 켜진다 (대소문자 구별, 서버와 같다)
//   취소·닫기는 탈퇴하지 않고 입력을 비운다
//   성공하면 서버가 리프레시 쿠키와 이 회원의 푸시 구독을 모두 지웠으므로, 브라우저 구독만 해제하고 로그인 화면으로 새로 연다.
//   새로 열면 메모리의 액세스 토큰과 캐시도 사라진다 (서버 API를 더 부르지 않는다. 부르면 A002로 막힌다)
export function normalizeNickname(value: string): string {
  return value.normalize("NFC").trim()
}

export function WithdrawDialog({
  open,
  onOpenChange,
  nickname,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  nickname: string
}) {
  const t = useTranslations("accountManagement.withdraw")
  const { describeError } = useErrorMessages()
  const [input, setInput] = useState("")
  const [nicknameError, setNicknameError] = useState(false)
  const [formError, setFormError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const matches = normalizeNickname(input) !== "" && normalizeNickname(input) === normalizeNickname(nickname)

  const changeOpen = (next: boolean) => {
    if (submitting) return
    if (!next) {
      setInput("")
      setNicknameError(false)
      setFormError(undefined)
    }
    onOpenChange(next)
  }

  const submit = async () => {
    setSubmitting(true)
    setNicknameError(false)
    setFormError(undefined)
    try {
      const { purgeAt } = await api.v1.withdrawal.withdraw(input)
      await unsubscribeBrowserOnly()
      window.location.replace(withdrawnSignInPath(purgeAt))
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.fieldErrors.some((fieldError) => fieldError.field === "nickname")) setNicknameError(true)
      else setFormError(describeError(apiError).message)
      setSubmitting(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={changeOpen}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("title")}</AlertDialogTitle>
          <AlertDialogDescription render={<div />}>
            <ul className="list-disc space-y-1 pl-5 text-left">
              <li>{t("noticeRecover")}</li>
              <li>{t("noticePurge")}</li>
              <li>{t("noticeContent")}</li>
            </ul>
          </AlertDialogDescription>
        </AlertDialogHeader>
        {formError && (
          <Alert variant="destructive">
            <AlertDescription>{formError}</AlertDescription>
          </Alert>
        )}
        <Field data-invalid={nicknameError}>
          <FieldLabel htmlFor="withdraw-nickname">{t("nicknameLabel")}</FieldLabel>
          <Input
            id="withdraw-nickname"
            value={input}
            autoComplete="off"
            onChange={(event) => {
              setInput(event.target.value)
              setNicknameError(false)
            }}
            aria-invalid={nicknameError}
            aria-describedby={nicknameError ? "withdraw-nickname-error" : "withdraw-nickname-help"}
          />
          <FieldDescription id="withdraw-nickname-help">{t("nicknameHelp", { nickname })}</FieldDescription>
          {nicknameError && <FieldError id="withdraw-nickname-error" errors={[{ message: t("nicknameMismatch") }]} />}
        </Field>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={submitting}>{t("cancel")}</AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={!matches || submitting} onClick={submit}>
            {t("confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
