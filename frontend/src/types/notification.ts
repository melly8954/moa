// 알림 API의 응답·요청 (specs/007-notifications contracts/notifications-api.md, contracts/realtime.md)

// INQUIRY_ANSWERED: 1:1 문의에 답변이 등록됐다 (specs/017-customer-center FR-012)
export type NotificationType = "SYSTEM" | "INQUIRY_ANSWERED"

export type NotificationItem = {
  id: number
  type: NotificationType
  title: string
  // 관련 화면 경로 (/로 시작)
  link: string
  createdAt: string
  read: boolean
}

export type UnreadCount = { count: number }
export type PushPublicKey = { publicKey: string | null }
export type PushSubscriptionRequest = { endpoint: string; keys: { p256dh: string; auth: string } }

// 실시간 메시지 (STOMP)
export type NoticePublishedMessage = { type: "NOTICE_PUBLISHED"; id: number; title: string; link: string }
export type NotificationCreatedMessage = {
  type: "NOTIFICATION_CREATED"
  id: number
  notificationType: NotificationType
  title: string
  link: string
  createdAt: string
}
