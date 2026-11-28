// 파티 API의 응답·요청 (specs/008-party contracts/party-api.md "응답 모양")
// 백엔드가 아직 떠 있지 않아 gen:api를 돌리지 못해 손으로 적는다. 백엔드가 생기면 Schema<"...">로 바꾼다

export type JoinPolicy = "OPEN" | "APPROVAL" | "PRIVATE"
export type PartyStatus = "RECRUITING" | "CLOSED"
export type PartyRole = "HOST" | "MEMBER"
export type PartyRelation = "NONE" | "MEMBER" | "HOST" | "PENDING" | "BANNED"

// 회원은 표시 이름으로만 나간다. 로그인 아이디는 싣지 않는다 (FR-027)
export type MemberRef = { userId: number; displayName: string }

export type OpenPosition = { positionCode: string; remaining: number }
export type PartyPosition = { positionCode: string; slots: number; filled: number }

export type PartyMember = MemberRef & {
  role: PartyRole
  positionCode: string | null
  tierCode: string | null
  joinedAt: string
}

// 모집 페이지 카드
export type PartySummary = {
  id: number
  gameCode: string
  modeCode: string | null
  title: string
  joinPolicy: JoinPolicy
  memberCount: number
  capacity: number
  openPositions: OpenPosition[]
  tierMinCode: string | null
  tierMaxCode: string | null
  host: MemberRef
  createdAt: string
}

export type PartyInvite = { code: string; url: string }

export type PartyDetail = PartySummary & {
  description: string | null
  status: PartyStatus
  closedByHost: boolean
  positionEnabled: boolean
  positions: PartyPosition[]
  members: PartyMember[]
  me: { relation: PartyRelation; joinRequestId?: number | null }
  // 멤버에게만 온다 (US4-AC1)
  invite?: PartyInvite | null
}

export type PartyQuery = { gameCode?: string; modeCode?: string }

// 만들기·고치기 (POST /parties, PUT /parties/{id}). 포지션이 있으면 capacity = slots 합 (방장 자리 포함, FR-022)
export type PartySaveRequest = {
  gameCode: string
  modeCode?: string
  title: string
  description?: string
  capacity: number
  joinPolicy: JoinPolicy
  tierMinCode?: string
  tierMaxCode?: string
  positions?: { positionCode: string; slots: number }[]
  hostPositionCode?: string
}

// 고치기 (PUT /parties/{id}, 백엔드 PartyModifyRequest). 참가 방식은 PUT /join-policy로 따로 바꾸므로 싣지 않는다
//   모든 칸을 보낸 값으로 바꾼다 (빠진 칸은 비운다)
export type PartyModifyRequest = Omit<PartySaveRequest, "joinPolicy">

// 참가·신청·초대 입장의 몸통
export type PartyJoinRequestBody = { positionCode?: string; tierCode?: string }

// 초대 입장 화면 (GET /parties/invites/{code})
export type InvitePreview = {
  id: number
  gameCode: string
  modeCode: string | null
  title: string
  host: MemberRef
  memberCount: number
  capacity: number
  positions: PartyPosition[]
  me: { relation: PartyRelation; joinRequestId?: number | null }
  joinable: boolean
  // 못 들어오는 이유 (오류 코드: B101, B102, B103, B104 등)
  reasonCode: string | null
}

// 내 파티 (GET /parties/me)
export type MyPartyJoinRequest = {
  joinRequestId: number
  party: { id: number; title: string; gameCode: string }
  positionCode: string | null
  appliedAt: string
}

export type MyParty = {
  party: (PartySummary & { myRole: PartyRole }) | null
  joinRequests: MyPartyJoinRequest[]
}

// 방장의 신청 목록 (GET /parties/{id}/join-requests). 대기 중만 신청순
export type PartyJoinRequestItem = {
  joinRequestId: number
  user: MemberRef
  positionCode: string | null
  tierCode: string | null
  appliedAt: string
}

// 채팅 (contracts/party-api.md "채팅"). SYSTEM은 systemEvent와 subject로 안내 문구를 만든다
export type PartyMessageType = "CHAT" | "SYSTEM"
export type PartySystemEvent = "JOINED" | "LEFT" | "KICKED" | "HOST_CHANGED"

export type PartyMessage = {
  id: number
  type: PartyMessageType
  sender: MemberRef | null
  content: string | null
  systemEvent: PartySystemEvent | null
  subject: MemberRef | null
  sentAt: string
}

export type PartyMessagePage = { items: PartyMessage[]; hasMore: boolean }

// 실시간 MEMBERS_CHANGED payload (contracts/party-realtime.md)
export type PartyMembersChanged = Pick<PartyDetail, "members" | "positions" | "memberCount" | "capacity" | "status"> & {
  hostUserId: number
}
