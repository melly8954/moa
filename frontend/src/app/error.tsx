"use client"

import { useTranslations } from "next-intl"
import { useEffect } from "react"
import { toApiError } from "@/api/error"
import { ErrorView } from "@/components/kit/error-view"

// 화면을 그리다 난 오류 (셸 안의 화면이면 셸은 그대로 두고 화면 자리에 보인다)
// API 오류는 화면 안에서 QueryState·폼 오류로 보이는 것이 기본이다. 여기는 그래도 새어 나온 오류다
// 서버 오류(S001)면 문의 번호(traceId)를 보인다
export default function ErrorPage({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  const t = useTranslations()

  useEffect(() => {
    console.error(error)
  }, [error])

  const apiError = toApiError(error)
  const traceId = apiError.code === "S001" ? apiError.traceId : undefined
  return (
    <ErrorView
      title={t("errorPage.title")}
      description={t("errorPage.description")}
      traceIdText={traceId && t("common.traceId", { traceId })}
      retryLabel={t("common.retry")}
      onRetry={retry}
    />
  )
}
