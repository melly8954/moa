import { QueryClient } from "@tanstack/react-query"
import { beforeEach, describe, expect, it, vi } from "vitest"
import type { PartyEvent } from "@/lib/party-realtime"
import type { PartyDetail, PartyMember, PartyMembersChanged, PartyMessage } from "@/types/party"

// moa: 파티 화면의 실시간 반영 (specs/008-party contracts/party-realtime.md "이벤트", T025·T055)
//   usePartyLive가 부르는 훅(구독, 라우터, 문구, 쿼리 클라이언트, 내 정보)을 모두 바꿔 끼우고 함수로 바로 부른다
//   구독에 넘긴 처리 함수를 잡아 두었다가 이벤트를 넣고, 진짜 QueryClient의 캐시가 어떻게 바뀌는지 본다

type Handler = (event: PartyEvent) => void
const h = vi.hoisted(() => ({
  queryClient: undefined as unknown as QueryClient,
  myId: 2 as number | undefined,
  channel: { partyId: null as number | null, onEvent: undefined as undefined | ((e: unknown) => void) },
  queue: undefined as undefined | ((e: unknown) => void),
  subscription: { destination: null as string | null, onMessage: undefined as undefined | (() => void) },
  replace: vi.fn(),
  toastInfo: vi.fn(),
}))

vi.mock("@tanstack/react-query", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@tanstack/react-query")>()),
  useQueryClient: () => h.queryClient,
}))
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: h.replace, push: vi.fn() }) }))
vi.mock("next-intl", () => ({ useTranslations: () => (key: string) => key }))
vi.mock("sonner", () => ({ toast: { info: h.toastInfo } }))
vi.mock("@/lib/me", () => ({ useMe: () => ({ data: h.myId == null ? undefined : { id: h.myId } }) }))
vi.mock("@/lib/party-realtime", () => ({
  usePartyChannel: (partyId: number | null, onEvent: (e: unknown) => void) => {
    h.channel.partyId = partyId
    h.channel.onEvent = onEvent
  },
  useMyPartyQueue: (onEvent: (e: unknown) => void) => {
    h.queue = onEvent
  },
}))
vi.mock("@/providers/realtime-provider", () => ({
  useRealtimeSubscription: (destination: string | null, onMessage: () => void) => {
    h.subscription.destination = destination
    h.subscription.onMessage = onMessage
  },
}))

const { usePartyLive, RECRUIT_PATH } = await import("@/app/(user)/parties/[id]/use-party-live")
// 바꿔 끼운 훅만 부르므로 React 밖에서 불러도 된다. 훅 규칙 검사가 이름으로 막아 다른 이름으로 부른다
const runPartyLive = usePartyLive
const { partyDetailKey, partyJoinRequestsKey, partyMessagesKey } = await import("@/app/(user)/parties/[id]/use-party")

const ID = 7
const AT = "2026-11-28T09:00:00+09:00"

const member = (userId: number, role: PartyMember["role"] = "MEMBER"): PartyMember => ({
  userId,
  displayName: `회원${userId}`,
  role,
  positionCode: null,
  tierCode: null,
  joinedAt: AT,
})

function detail(relation: PartyDetail["me"]["relation"] = "MEMBER"): PartyDetail {
  return {
    id: ID,
    gameCode: "LOL",
    modeCode: null,
    title: "같이 해요",
    joinPolicy: "OPEN",
    memberCount: 2,
    capacity: 3,
    openPositions: [],
    tierMinCode: null,
    tierMaxCode: null,
    host: { userId: 1, displayName: "회원1" },
    createdAt: AT,
    description: null,
    status: "RECRUITING",
    closedByHost: false,
    positionEnabled: false,
    positions: [],
    members: [member(1, "HOST"), member(2)],
    me: { relation },
    invite: { code: "ABCD1234", url: "/invite/ABCD1234" },
  }
}

const chat = (id: number): PartyMessage => ({
  id,
  type: "CHAT",
  sender: { userId: 1, displayName: "회원1" },
  content: `메시지 ${id}`,
  systemEvent: null,
  subject: null,
  sentAt: AT,
})

const event = <P>(type: PartyEvent["type"], payload: P, partyId = ID): PartyEvent<P> => ({
  type,
  partyId,
  payload,
  occurredAt: AT,
})

// 화면을 그린 것처럼 훅을 한 번 부른다. 캐시에 파티를 넣고 그 값을 넘긴다
function mount(party: PartyDetail | undefined = detail()) {
  if (party) h.queryClient.setQueryData(partyDetailKey(ID), party)
  runPartyLive(ID, party)
}

const channel = (e: PartyEvent) => (h.channel.onEvent as Handler)(e)
const queue = (e: PartyEvent) => (h.queue as Handler)(e)
const cached = () => h.queryClient.getQueryData<PartyDetail>(partyDetailKey(ID))

beforeEach(() => {
  h.queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  h.myId = 2
  h.channel.partyId = null
  h.channel.onEvent = undefined
  h.queue = undefined
  h.subscription.destination = null
  h.replace.mockReset()
  h.toastInfo.mockReset()
})

describe("파티 화면 실시간 반영", () => {
  it("멤버일 때만 파티 주소를 구독한다", () => {
    mount(detail("MEMBER"))
    expect(h.channel.partyId).toBe(ID)
    mount(detail("HOST"))
    expect(h.channel.partyId).toBe(ID)
    mount(detail("NONE"))
    expect(h.channel.partyId).toBeNull()
    runPartyLive(ID, undefined)
    expect(h.channel.partyId).toBeNull()
  })

  it("MESSAGE: 채팅 캐시 끝에 붙이고, 이미 있는 id는 다시 붙이지 않는다", () => {
    mount()
    h.queryClient.setQueryData(partyMessagesKey(ID), { items: [chat(1), chat(2)], hasOlder: false })

    channel(event("MESSAGE", chat(3)))
    channel(event("MESSAGE", chat(3)))

    const items = h.queryClient.getQueryData<{ items: PartyMessage[] }>(partyMessagesKey(ID))?.items
    expect(items?.map((m) => m.id)).toEqual([1, 2, 3])
  })

  it("다른 파티의 이벤트는 무시한다", () => {
    mount()
    h.queryClient.setQueryData(partyMessagesKey(ID), { items: [chat(1)], hasOlder: false })
    channel(event("MESSAGE", chat(2), ID + 1))
    channel(event("PARTY_UPDATED", { title: "다른 파티" }, ID + 1))
    expect(h.queryClient.getQueryData<{ items: PartyMessage[] }>(partyMessagesKey(ID))?.items).toHaveLength(1)
    expect(cached()?.title).toBe("같이 해요")
  })

  it("MEMBERS_CHANGED: 멤버·인원·상태·포지션·빈 포지션·방장을 고치고, 내가 방장이 되면 relation이 HOST다", () => {
    mount()
    const payload: PartyMembersChanged = {
      members: [member(2, "HOST"), member(3)],
      positions: [
        { positionCode: "MID", slots: 1, filled: 1 },
        { positionCode: "TOP", slots: 2, filled: 1 },
      ],
      memberCount: 2,
      capacity: 3,
      status: "CLOSED",
      hostUserId: 2,
    }

    channel(event("MEMBERS_CHANGED", payload))

    const party = cached()
    expect(party?.members.map((m) => m.userId)).toEqual([2, 3])
    expect(party?.positions).toEqual(payload.positions)
    expect(party?.memberCount).toBe(2)
    expect(party?.status).toBe("CLOSED")
    expect(party?.openPositions).toEqual([{ positionCode: "TOP", remaining: 1 }])
    expect(party?.host).toEqual(member(2, "HOST"))
    expect(party?.me.relation).toBe("HOST")
    // 멤버에게만 오는 초대는 그대로 남는다
    expect(party?.invite?.code).toBe("ABCD1234")
  })

  it("MEMBERS_CHANGED: 방장이 아니면 relation은 MEMBER다", () => {
    h.myId = 1
    mount(detail("HOST"))
    channel(
      event<PartyMembersChanged>("MEMBERS_CHANGED", {
        members: [member(1), member(2, "HOST")],
        positions: [],
        memberCount: 2,
        capacity: 3,
        status: "RECRUITING",
        hostUserId: 2,
      }),
    )
    expect(cached()?.me.relation).toBe("MEMBER")
    expect(cached()?.host.userId).toBe(2)
  })

  it("MEMBERS_CHANGED: 멤버 목록에 내가 없으면 캐시를 고치지 않고 다시 읽게 한다", () => {
    mount()
    const invalidate = vi.spyOn(h.queryClient, "invalidateQueries")
    channel(
      event<PartyMembersChanged>("MEMBERS_CHANGED", {
        members: [member(1, "HOST")],
        positions: [],
        memberCount: 1,
        capacity: 3,
        status: "RECRUITING",
        hostUserId: 1,
      }),
    )
    expect(invalidate).toHaveBeenCalledWith({ queryKey: partyDetailKey(ID), exact: true })
    expect(cached()?.members).toHaveLength(2)
  })

  it("PARTY_UPDATED: 받은 칸(초대 코드 포함)을 덮어쓰고 me는 그대로 둔다", () => {
    mount()
    channel(
      event("PARTY_UPDATED", {
        title: "새 제목",
        capacity: 5,
        invite: { code: "NEWCODE1", url: "/invite/NEWCODE1" },
        me: { relation: "NONE" },
      }),
    )
    expect(cached()?.title).toBe("새 제목")
    expect(cached()?.capacity).toBe(5)
    expect(cached()?.invite?.code).toBe("NEWCODE1")
    expect(cached()?.me.relation).toBe("MEMBER")
  })

  it("PARTY_DISSOLVED: 멤버는 알림을 보고 모집 페이지로 가며 파티 캐시를 지운다", () => {
    mount()
    channel(event("PARTY_DISSOLVED", {}))
    expect(h.toastInfo).toHaveBeenCalledWith("dissolved")
    expect(h.replace).toHaveBeenCalledWith(RECRUIT_PATH)
    expect(cached()).toBeUndefined()
  })

  it("PARTY_DISSOLVED: 방장 화면에서는 아무것도 하지 않는다 (방장 동작이 옮겨 보낸다)", () => {
    mount(detail("HOST"))
    channel(event("PARTY_DISSOLVED", {}))
    expect(h.toastInfo).not.toHaveBeenCalled()
    expect(h.replace).not.toHaveBeenCalled()
    expect(cached()).toBeDefined()
  })

  it("개인 KICKED: 이 파티면 알림을 보고 모집 페이지로 간다. 다른 파티면 무시한다", () => {
    mount()
    queue(event("KICKED", {}, ID + 1))
    expect(h.replace).not.toHaveBeenCalled()

    queue(event("KICKED", {}))
    expect(h.toastInfo).toHaveBeenCalledWith("kicked")
    expect(h.replace).toHaveBeenCalledWith(RECRUIT_PATH)
    expect(cached()).toBeUndefined()
  })

  it("개인 JOIN_REQUESTS_CHANGED: 이 파티의 신청 목록을 다시 읽게 한다", () => {
    mount(detail("HOST"))
    const invalidate = vi.spyOn(h.queryClient, "invalidateQueries")
    queue(event("JOIN_REQUESTS_CHANGED", {}, ID + 1))
    expect(invalidate).not.toHaveBeenCalled()
    queue(event("JOIN_REQUESTS_CHANGED", {}))
    expect(invalidate).toHaveBeenCalledWith({ queryKey: partyJoinRequestsKey(ID) })
  })

  it("신청 대기 중이면 개인 알림을 구독하고, 알림이 오면 파티를 다시 읽게 한다", () => {
    mount(detail("MEMBER"))
    expect(h.subscription.destination).toBeNull()

    mount(detail("PENDING"))
    expect(h.subscription.destination).toBe("/user/queue/notifications")
    const invalidate = vi.spyOn(h.queryClient, "invalidateQueries")
    h.subscription.onMessage?.()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: partyDetailKey(ID), exact: true })
  })
})
