"use client"

import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useEffect, useState } from "react"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { LegalFooter } from "@/components/legal/legal-footer"
import { fetchLandingPath } from "@/components/shell/landing"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import { currentNextPath, SIGN_IN_PATH } from "@/lib/next-path"
import { useAuthStore } from "@/stores/auth"

// 쿠키가 없거나 맞지 않음(이미 복구함·다시 탈퇴함). 로그인 화면으로 돌려보낸다
const NOT_AVAILABLE = "A403"

// moa: 탈퇴한 계정 복구 (specs/018-member-withdrawal contracts/screens.md, US2-AC1·AC2·AC4·AC5)
//   로그인(아이디·비밀번호 A401, 소셜 콜백)이 심은 복구 쿠키(HttpOnly)로 완전 삭제 예정 시각을 읽는다
//   "복구하기": 로그인한 때와 같이 ?next= 또는 첫 화면으로. "취소": 탈퇴 상태 그대로 로그인 화면으로
//   쿠키가 없거나 맞지 않으면(A403) 로그인 화면으로 보낸다
export default function RecoverPage() {
  const t = useTranslations("recover")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const recovery = useQuery({ queryKey: ["recovery"], queryFn: api.v1.withdrawal.recovery, retry: false })
  const loadError = recovery.error ? toApiError(recovery.error) : undefined

  useEffect(() => {
    if (loadError?.code === NOT_AVAILABLE) router.replace(SIGN_IN_PATH)
  }, [loadError?.code, router])

  const recover = async () => {
    setFormError(undefined)
    setSubmitting(true)
    try {
      const token = await api.v1.withdrawal.recover()
      queryClient.clear()
      useAuthStore.getState().signIn(token.accessToken)
      const next = currentNextPath()
      router.replace(next ?? (await fetchLandingPath(queryClient)))
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.code === NOT_AVAILABLE) {
        router.replace(SIGN_IN_PATH)
        return
      }
      setFormError(describeError(apiError).message)
      setSubmitting(false)
    }
  }

  const errorMessage =
    formError ?? (loadError && loadError.code !== NOT_AVAILABLE ? describeError(loadError).message : undefined)

  // 맨 아래 푸터 줄 (specs/012-terms-privacy FR-004, US2-AC1)
  return (
    <>
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>
            <h1>{t("title")}</h1>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {errorMessage && (
            <Alert variant="destructive">
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}
          {recovery.isPending ? (
            <Skeleton className="h-10 w-full" />
          ) : (
            recovery.data && (
              <p className="text-sm">{t("description", { date: formatDateTime(recovery.data.purgeAt) })}</p>
            )
          )}
        </CardContent>
        <CardFooter className="justify-end gap-2">
          <Button variant="outline" disabled={submitting} onClick={() => router.replace(SIGN_IN_PATH)}>
            {t("cancel")}
          </Button>
          <Button disabled={!recovery.data || submitting} onClick={recover}>
            {t("submit")}
          </Button>
        </CardFooter>
      </Card>
      <LegalFooter className="mt-4" />
    </>
  )
}
