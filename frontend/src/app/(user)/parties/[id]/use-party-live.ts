"use client"

import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { type PartyEvent, useMyPartyQueue, usePartyChannel } from "@/lib/party-realtime"
import { useMe } from "@/lib/me"
import { useRealtimeSubscription } from "@/providers/realtime-provider"
import type { PartyDetail, PartyMembersChanged, PartyMessage } from "@/types/party"
import { appendMessages, isMember, partyDetailKey, partyJoinRequestsKey } from "./use-party"

export const RECRUIT_PATH = "/recruit"

// 파티 화면의 실시간 반영 (T025, contracts/party-realtime.md "이벤트")
//   멤버일 때만 /topic/parties/{id}를 구독한다. 아니면 null (막힌 구독이 알림과 함께 쓰는 연결을 끊을 수 있다, research.md R3)
//   MESSAGE: 채팅 끝에 붙인다 / MEMBERS_CHANGED·PARTY_UPDATED: 파티 캐시를 고친다
//   PARTY_DISSOLVED, 개인 KICKED: 알리고 모집 페이지로 보낸다. 해산은 방장이 한 일이라 방장 화면에는 알리지 않는다(방장 동작이 옮겨 보낸다)
//   개인 JOIN_REQUESTS_CHANGED: 방장의 신청 목록을 다시 읽는다
//   신청 대기 중이면 개인 알림(승인·거절, 007 /user/queue/notifications)이 올 때 파티 화면을 다시 읽는다
//   다시 이어진 뒤 빠진 메시지 채우기는 RealtimeProvider가 ["parties"]를 다시 읽을 때 채팅 쿼리가 after로 한다 (use-party.ts)
export function usePartyLive(id: number, party: PartyDetail | undefined) {
  const t = useTranslations("party.live")
  const router = useRouter()
  const queryClient = useQueryClient()
  const myId = useMe().data?.id
  const member = isMember(party)
  const relation = party?.me.relation

  const leaveScreen = (message: string) => {
    toast.info(message)
    queryClient.removeQueries({ queryKey: partyDetailKey(id) })
    router.replace(RECRUIT_PATH)
  }

  usePartyChannel(member ? id : null, (event: PartyEvent) => {
    if (event.partyId !== id) return
    switch (event.type) {
      case "MESSAGE":
        appendMessages(queryClient, id, [event.payload as PartyMessage])
        return
      case "MEMBERS_CHANGED": {
        const payload = event.payload as PartyMembersChanged
        const stillMember = myId == null || payload.members.some((m) => m.userId === myId)
        if (!stillMember) {
          void queryClient.invalidateQueries({ queryKey: partyDetailKey(id), exact: true })
          return
        }
        queryClient.setQueryData<PartyDetail>(partyDetailKey(id), (old) =>
          old
            ? {
                ...old,
                members: payload.members,
                positions: payload.positions,
                memberCount: payload.memberCount,
                capacity: payload.capacity,
                status: payload.status,
                openPositions: payload.positions
                  .filter((p) => p.filled < p.slots)
                  .map((p) => ({ positionCode: p.positionCode, remaining: p.slots - p.filled })),
                host: payload.members.find((m) => m.userId === payload.hostUserId) ?? old.host,
                me: { ...old.me, relation: payload.hostUserId === myId ? "HOST" : "MEMBER" },
              }
            : old,
        )
        return
      }
      case "PARTY_UPDATED":
        queryClient.setQueryData<PartyDetail>(partyDetailKey(id), (old) =>
          old ? { ...old, ...(event.payload as Partial<PartyDetail>), me: old.me, invite: old.invite } : old,
        )
        return
      case "PARTY_DISSOLVED":
        if (relation === "HOST") return
        leaveScreen(t("dissolved"))
        return
      default:
        return
    }
  })

  useMyPartyQueue((event: PartyEvent) => {
    if (event.type === "KICKED" && event.partyId === id) leaveScreen(t("kicked"))
    if (event.type === "JOIN_REQUESTS_CHANGED" && event.partyId === id) {
      void queryClient.invalidateQueries({ queryKey: partyJoinRequestsKey(id) })
    }
  })

  useRealtimeSubscription<unknown>(relation === "PENDING" ? "/user/queue/notifications" : null, () => {
    void queryClient.invalidateQueries({ queryKey: partyDetailKey(id), exact: true })
  })
}
