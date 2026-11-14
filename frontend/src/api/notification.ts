import type { PageRequest, PageResponse } from "@/types/api"
import type { NotificationItem, PushPublicKey, PushSubscriptionRequest, UnreadCount } from "@/types/notification"
import { http } from "./client"

// 내 알림함과 브라우저 알림 구독 (specs/007-notifications). api.v1.notification으로 부른다. 늘 내 것만
const base = "/api/v1/notifications"

export const notificationApi = {
  getPage: (params?: PageRequest) => http.get<PageResponse<NotificationItem>>(base, { params }).then((res) => res.data),
  unreadCount: () => http.get<UnreadCount>(`${base}/unread-count`).then((res) => res.data),
  read: (id: number) => http.put<void>(`${base}/${id}/read`).then(() => undefined),
  readAll: () => http.put<void>(`${base}/read-all`).then(() => undefined),
  pushPublicKey: () => http.get<PushPublicKey>(`${base}/push/public-key`).then((res) => res.data),
  subscribePush: (data: PushSubscriptionRequest) =>
    http.post<void>(`${base}/push-subscriptions`, data).then(() => undefined),
  unsubscribePush: (endpoint: string) =>
    http.delete<void>(`${base}/push-subscriptions`, { params: { endpoint } }).then(() => undefined),
}
