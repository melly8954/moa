"use client"

import type { UseQueryResult } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { useErrorMessages } from "@/lib/error-messages"

// 조회 결과의 세 상태를 같은 모양으로 보인다: 불러오는 중 · 오류 · 빈 결과
// 데이터가 있으면 children(data)를 그린다
//   <QueryState query={orders} isEmpty={(d) => d.content.length === 0}>{(d) => <OrderTable rows={d.content} />}</QueryState>
// 다시 불러오는 동안(이미 데이터가 있을 때)은 그대로 둔 데이터를 보인다
export function QueryState<T>({
  query,
  isEmpty,
  empty,
  loading,
  wrapError = (alert) => alert,
  children,
}: {
  query: Pick<UseQueryResult<T>, "data" | "error" | "isPending" | "isError" | "refetch">
  isEmpty?: (data: T) => boolean
  // 빈 결과 문구. 기본은 common.empty
  empty?: React.ReactNode
  // 불러오는 중 모양. 기본은 회색 막대 세 줄
  loading?: React.ReactNode
  // 오류 안내를 감쌀 틀 (여백 등). 기본은 그대로
  wrapError?: (alert: React.ReactNode) => React.ReactNode
  children: (data: T) => React.ReactNode
}) {
  const t = useTranslations("common")
  const { describeError } = useErrorMessages()

  if (query.isPending) {
    return (
      loading ?? (
        <div className="flex flex-col gap-2" aria-busy="true">
          <span className="sr-only">{t("loading")}</span>
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-2/3" />
        </div>
      )
    )
  }

  if (query.isError) {
    const { message, traceId } = describeError(query.error)
    return wrapError(
      <Alert variant="destructive" role="alert">
        <AlertDescription>
          {message}
          {traceId && <span className="block text-xs">{t("traceId", { traceId })}</span>}
        </AlertDescription>
        <AlertAction>
          <Button variant="outline" size="sm" onClick={() => void query.refetch()}>
            {t("retry")}
          </Button>
        </AlertAction>
      </Alert>,
    )
  }

  const data = query.data as T
  if (isEmpty?.(data)) {
    return (
      <Empty>
        <EmptyHeader>
          <EmptyDescription>{empty ?? t("empty")}</EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  }

  return children(data)
}
