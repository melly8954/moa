"use client"

import { Bell } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { LinkButton } from "@/components/kit/link-button"
import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { formatDateTime } from "@/lib/format"
import { cn } from "@/lib/utils"
import { unreadLabel } from "@/lib/notification"
import { useOpenNotification, useReadAll, useRecentNotifications, useUnreadCount } from "./use-notifications"

// 알림 벨 (specs/007-notifications US2, contracts/notifications-screens.md)
//   안 읽은 수(99+), 누르면 최근 10개 + "모두 읽음" + "모두 보기"(/notifications)
//   trigger: 셸마다 다른 버튼 모양 (사이드바 계정 줄은 SidebarMenuButton, 좁은 폭 머리는 아이콘 버튼)
//   showLabel: 사이드바처럼 "알림" 글자를 함께 보인다
export function NotificationBell({ trigger, showLabel = false }: { trigger: React.ReactElement; showLabel?: boolean }) {
  const t = useTranslations("notification")
  const [open, setOpen] = useState(false)
  const count = useUnreadCount()
  const recent = useRecentNotifications(open)
  const openNotification = useOpenNotification()
  const readAll = useReadAll()
  const label = unreadLabel(count)
  const name = count > 0 ? t("bellUnread", { count }) : t("bell")

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger render={trigger} aria-label={name}>
        <span className="relative inline-flex">
          <Bell aria-hidden />
          {label && (
            <span
              aria-hidden
              className="absolute -top-1.5 -right-2 min-w-4 rounded-full bg-primary px-1 text-center text-[10px] leading-4 font-medium text-primary-foreground"
            >
              {label}
            </span>
          )}
        </span>
        {showLabel && <span>{t("bell")}</span>}
      </PopoverTrigger>
      <PopoverContent align="start" className="w-80 p-0">
        <div className="flex items-center justify-between gap-2 border-b px-3 py-2">
          <span className="text-sm font-medium">{t("recent")}</span>
          <Button variant="ghost" size="sm" disabled={count === 0} onClick={() => readAll.mutate()}>
            {t("readAll")}
          </Button>
        </div>
        {recent.data && recent.data.content.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-muted-foreground">{t("empty")}</p>
        ) : (
          <ul className="max-h-80 divide-y overflow-y-auto">
            {recent.data?.content.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className="flex w-full flex-col gap-0.5 px-3 py-2 text-left hover:bg-muted focus-visible:bg-muted"
                  onClick={() => {
                    setOpen(false)
                    void openNotification(n)
                  }}
                >
                  <span className={cn("flex items-center gap-2 text-sm", !n.read && "font-medium")}>
                    {!n.read && <span className="size-2 shrink-0 rounded-full bg-destructive" aria-hidden />}
                    <span className="min-w-0 truncate">{n.title}</span>
                    {!n.read && <span className="sr-only">{t("unread")}</span>}
                  </span>
                  <span className="text-xs text-muted-foreground">{formatDateTime(n.createdAt)}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <div className="border-t p-2">
          <LinkButton href="/notifications" variant="ghost" size="sm" className="w-full" onClick={() => setOpen(false)}>
            {t("viewAll")}
          </LinkButton>
        </div>
      </PopoverContent>
    </Popover>
  )
}
