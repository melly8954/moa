// 알림 API의 응답·요청 (specs/007-notifications contracts/notifications-api.md, contracts/realtime.md)

// moa: 파티 알림 6종 (specs/008-party FR-029). 백엔드 NotificationType과 같다. gen:api를 돌리면 생성 타입과 맞춘다
export type NotificationType =
  | "SYSTEM"
  | "PARTY_APPLIED"
  | "PARTY_APPROVED"
  | "PARTY_REJECTED"
  | "PARTY_HOST_CHANGED"
  | "PARTY_KICKED"
  | "PARTY_DISSOLVED"

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
