import { useRealtimeSubscription } from "@/providers/realtime-provider"

// 파티 실시간 이벤트 (specs/008-party contracts/party-realtime.md "이벤트")
//   모양은 { type, partyId, payload, occurredAt }이다. 커밋 뒤에만 온다
export type PartyEventType =
  "MESSAGE" | "MEMBERS_CHANGED" | "PARTY_UPDATED" | "PARTY_DISSOLVED" | "KICKED" | "JOIN_REQUESTS_CHANGED"

export type PartyEvent<P = unknown> = {
  type: PartyEventType
  partyId: number
  payload: P
  occurredAt: string
}

// 파티 주소 구독. 멤버가 아니면 partyId에 null을 넘겨 구독하지 않는다 (막힌 구독은 알림과 함께 쓰는 연결을 끊을 수 있다)
export function usePartyChannel(partyId: number | null, onEvent: (event: PartyEvent) => void) {
  useRealtimeSubscription<PartyEvent>(partyId == null ? null : `/topic/parties/${partyId}`, onEvent)
}

// 나에게만 오는 파티 일 (내보내짐, 방장에게 오는 신청 변화)
export function useMyPartyQueue(onEvent: (event: PartyEvent) => void) {
  useRealtimeSubscription<PartyEvent>("/user/queue/parties", onEvent)
}
