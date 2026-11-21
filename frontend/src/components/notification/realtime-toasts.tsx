"use client"

import { useQueryClient } from "@tanstack/react-query"
import { usePathname, useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { NEW_NOTICE_QUERY_KEY } from "@/components/notice/use-new-notice"
import { useRealtimeSubscription } from "@/providers/realtime-provider"
import type { NoticePublishedMessage, NotificationCreatedMessage } from "@/types/notification"
import { isOnRelatedScreen, safeLink } from "@/lib/notification"
import { NOTIFICATIONS_KEY, useOpenNotification } from "./use-notifications"

// 실시간 메시지를 토스트로 알리고 표시를 바꾼다 (specs/007-notifications US1·US3, contracts/notifications-screens.md)
//   공지 게시(/topic/notices): "새 공지: 제목" + "공지" 메뉴 새 공지 점 (006 US4)
//   개인 알림(/user/queue/notifications): "제목" + 벨 숫자. 관련 화면을 보고 있으면 토스트 없이 벨 숫자만 (FR-007, research R14)
//   토스트의 "보기"를 누르면 관련 화면으로 간다 (개인 알림은 읽음 처리 뒤)
//   알림 토스트는 파랑(toast.info)이다. 초록(toast.success)은 내가 한 일의 결과 안내에만 쓴다
export function RealtimeToasts() {
  const t = useTranslations("notification")
  const router = useRouter()
  const pathname = usePathname()
  const queryClient = useQueryClient()
  const openNotification = useOpenNotification()

  useRealtimeSubscription<NoticePublishedMessage>("/topic/notices", (message) => {
    void queryClient.invalidateQueries({ queryKey: NEW_NOTICE_QUERY_KEY })
    toast.info(t("noticeToast", { title: message.title }), {
      action: { label: t("open"), onClick: () => router.push(safeLink(message.link)) },
    })
  })

  useRealtimeSubscription<NotificationCreatedMessage>("/user/queue/notifications", (message) => {
    void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY })
    if (isOnRelatedScreen(message, pathname)) return
    toast.info(message.title, {
      action: {
        label: t("open"),
        onClick: () => void openNotification({ id: message.id, link: message.link, read: false }),
      },
    })
  })

  return null
}
