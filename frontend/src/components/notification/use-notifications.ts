"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { api } from "@/api"
import { safeLink } from "@/lib/notification"
import { useAuthStore } from "@/stores/auth"
import type { NotificationItem } from "@/types/notification"

// 알림 쿼리와 동작 (specs/007-notifications US2). 쿼리 키는 모두 ["notifications", ...]라 한 번에 무효화한다
export const NOTIFICATIONS_KEY = ["notifications"] as const
export const UNREAD_COUNT_KEY = ["notifications", "unread"] as const
export const RECENT_KEY = ["notifications", "recent"] as const
export const RECENT_SIZE = 10

export function useUnreadCount(): number {
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const query = useQuery({ queryKey: UNREAD_COUNT_KEY, queryFn: api.v1.notification.unreadCount, enabled: signedIn })
  return query.data?.count ?? 0
}

export function useRecentNotifications(enabled: boolean) {
  return useQuery({
    queryKey: RECENT_KEY,
    queryFn: () => api.v1.notification.getPage({ page: 0, size: RECENT_SIZE }),
    enabled,
  })
}

// 알림 누르기: 읽음으로 바꾼 뒤(이미 읽었으면 그대로) 관련 화면으로 간다
export function useOpenNotification() {
  const router = useRouter()
  const queryClient = useQueryClient()
  return async (notification: Pick<NotificationItem, "id" | "link" | "read">) => {
    if (!notification.read) {
      await api.v1.notification.read(notification.id).catch(() => undefined)
      await queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY })
    }
    router.push(safeLink(notification.link))
  }
}

export function useReadAll() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.v1.notification.readAll,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY }),
  })
}
