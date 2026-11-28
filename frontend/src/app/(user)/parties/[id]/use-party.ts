"use client"

import { type QueryClient, useQuery, useQueryClient } from "@tanstack/react-query"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { useAuthStore } from "@/stores/auth"
import type { PartyDetail, PartyMessage } from "@/types/party"

// 파티 화면의 쿼리 키. 모두 ["parties"]로 시작해 다시 이어질 때 RealtimeProvider가 다시 읽는다 (research.md R5)
export const partyDetailKey = (id: number) => ["parties", "detail", id] as const
export const partyMessagesKey = (id: number) => ["parties", "detail", id, "messages"] as const
export const partyJoinRequestsKey = (id: number) => ["parties", "detail", id, "join-requests"] as const

export const MESSAGE_PAGE_SIZE = 50

// 채팅 캐시. 오래된 것이 앞이다. hasOlder면 위로 올려 before로 더 불러온다
export type ChatCache = { items: PartyMessage[]; hasOlder: boolean }

export function isMember(party: PartyDetail | undefined) {
  return party?.me.relation === "MEMBER" || party?.me.relation === "HOST"
}

// 파티 화면. 로그인 확인이 끝난 뒤에 부른다 (me.relation이 로그인 여부에 따라 다르다)
//   없는 파티·해산·비멤버의 비공개(R001)는 다시 시도하지 않는다
export function useParty(id: number) {
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: partyDetailKey(id),
    queryFn: () => api.v1.party.get(id),
    enabled: Number.isInteger(id) && status !== "unknown",
    retry: (count, error) => toApiError(error).code !== "R001" && count < 2,
  })
}

// 새 메시지를 채팅 캐시 끝에 붙인다. 이미 있는 id는 건너뛴다 (실시간과 보내기 응답이 겹칠 수 있다)
export function appendMessages(queryClient: QueryClient, id: number, messages: PartyMessage[]) {
  queryClient.setQueryData<ChatCache>(partyMessagesKey(id), (old) => {
    if (!old) return old
    const known = new Set(old.items.map((m) => m.id))
    const fresh = messages.filter((m) => !known.has(m.id))
    if (fresh.length === 0) return old
    return { ...old, items: [...old.items, ...fresh].sort((a, b) => a.id - b.id) }
  })
}

// 채팅. 처음에는 마지막 50개를 받는다
//   캐시가 이미 있으면(다시 이어져 다시 읽을 때) 마지막 id 뒤의 것만 after로 채워 붙인다 (FR-018, research.md R5)
export function usePartyMessages(id: number, enabled: boolean) {
  const queryClient = useQueryClient()
  return useQuery({
    queryKey: partyMessagesKey(id),
    enabled,
    staleTime: Infinity,
    queryFn: async (): Promise<ChatCache> => {
      const old = queryClient.getQueryData<ChatCache>(partyMessagesKey(id))
      const last = old?.items.at(-1)
      if (!old || !last) {
        const page = await api.v1.party.messages(id, { size: MESSAGE_PAGE_SIZE })
        return { items: page.items, hasOlder: page.hasMore }
      }
      const fresh: PartyMessage[] = []
      let after = last.id
      for (;;) {
        const page = await api.v1.party.messages(id, { after, size: MESSAGE_PAGE_SIZE })
        fresh.push(...page.items)
        const tail = page.items.at(-1)
        if (!page.hasMore || !tail) break
        after = tail.id
      }
      const known = new Set(old.items.map((m) => m.id))
      return { ...old, items: [...old.items, ...fresh.filter((m) => !known.has(m.id))] }
    },
  })
}

// 위로 올렸을 때 더 오래된 50개를 앞에 붙인다
export async function loadOlderMessages(queryClient: QueryClient, id: number) {
  const old = queryClient.getQueryData<ChatCache>(partyMessagesKey(id))
  const first = old?.items[0]
  if (!old || !first || !old.hasOlder) return
  const page = await api.v1.party.messages(id, { before: first.id, size: MESSAGE_PAGE_SIZE })
  queryClient.setQueryData<ChatCache>(partyMessagesKey(id), (current) => {
    if (!current) return current
    const known = new Set(current.items.map((m) => m.id))
    return { items: [...page.items.filter((m) => !known.has(m.id)), ...current.items], hasOlder: page.hasMore }
  })
}
