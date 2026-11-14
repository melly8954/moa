"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { api } from "@/api"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { PushToggle } from "@/components/notification/push-toggle"
import { NOTIFICATIONS_KEY, useOpenNotification, useReadAll } from "@/components/notification/use-notifications"
import { Button } from "@/components/ui/button"
import { formatDateTime } from "@/lib/format"
import { cn } from "@/lib/utils"

const SIZE = 20

// 알림 화면 (specs/007-notifications US2·US4). 내 개인 알림 최신순, 모두 읽음, 브라우저 알림 켜기·끄기
// 알림을 누르면 읽음 처리 뒤 관련 화면으로 간다
export default function NotificationsPage() {
  const t = useTranslations("notification")
  const tt = useTranslations("table")
  const [page, setPage] = useState(0)
  const openNotification = useOpenNotification()
  const readAll = useReadAll()
  const notifications = useQuery({
    queryKey: [...NOTIFICATIONS_KEY, "page", page],
    queryFn: () => api.v1.notification.getPage({ page, size: SIZE }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        actions={
          <Button variant="outline" onClick={() => readAll.mutate()} disabled={readAll.isPending}>
            {t("readAll")}
          </Button>
        }
      />
      <PushToggle />
      <QueryState query={notifications} isEmpty={(data) => data.content.length === 0} empty={t("empty")}>
        {(data) => (
          <>
            <ul className="flex flex-col divide-y rounded-md border">
              {data.content.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className="flex w-full min-w-0 items-center gap-3 px-4 py-3 text-left hover:bg-muted focus-visible:bg-muted"
                    onClick={() => void openNotification(n)}
                  >
                    {!n.read && <span className="size-2 shrink-0 rounded-full bg-destructive" aria-hidden />}
                    <span className={cn("min-w-0 flex-1 truncate", !n.read && "font-medium")}>
                      {n.title}
                      {!n.read && <span className="sr-only">, {t("unread")}</span>}
                    </span>
                    <time dateTime={n.createdAt} className="shrink-0 text-sm text-muted-foreground">
                      {formatDateTime(n.createdAt)}
                    </time>
                  </button>
                </li>
              ))}
            </ul>
            {data.totalPages > 1 && (
              <nav aria-label={tt("pagination")} className="flex items-center justify-center gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  {tt("previous")}
                </Button>
                <span className="text-sm">
                  {page + 1} / {data.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  {tt("next")}
                </Button>
              </nav>
            )}
          </>
        )}
      </QueryState>
    </>
  )
}
