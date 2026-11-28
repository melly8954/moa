import type { PageRequest, PageResponse } from "@/types/api"
import type {
  InvitePreview,
  JoinPolicy,
  MyParty,
  PartyJoinRequestBody,
  PartyQuery,
  PartySummary,
  PartyDetail,
  PartyInvite,
  PartyJoinRequestItem,
  PartyMessage,
  PartyMessagePage,
  PartyModifyRequest,
  PartySaveRequest,
} from "@/types/party"
import { http } from "./client"

// 파티 (specs/008-party contracts/party-api.md)
//   api.v1.party: 모집 페이지·파티 화면·참가·채팅. 함수는 각 스토리의 화면 작업이 쓰는 경로만큼 더한다
const base = "/api/v1/parties"

export const partyApi = {
  // 모집 페이지 (T019). 로그인 없이 연다. 모집 중인 공개·승인 파티만 최신순
  getPage: (params?: PartyQuery & PageRequest) =>
    http.get<PageResponse<PartySummary>>(base, { params, skipAuth: true }).then((res) => res.data),
  // 만들기 (T017). 만든 회원이 방장이다
  create: (data: PartySaveRequest) => http.post<PartyDetail>(base, data).then((res) => res.data),
  // 초대 입장 (T027). 코드는 대소문자를 가리지 않는다
  getInvite: (code: string) =>
    http.get<InvitePreview>(`${base}/invites/${encodeURIComponent(code)}`).then((res) => res.data),
  joinByInvite: (code: string, data: PartyJoinRequestBody = {}) =>
    http.post<PartyDetail>(`${base}/invites/${encodeURIComponent(code)}/members`, data).then((res) => res.data),
  // 내 파티 (T038). 속한 파티와 대기 중인 신청
  me: () => http.get<MyParty>(`${base}/me`).then((res) => res.data),
  // 파티 화면 (T020). 로그인 없이도 열린다. 토큰이 있으면 실어 me.relation을 받는다
  get: (id: number) => http.get<PartyDetail>(`${base}/${id}`).then((res) => res.data),
  modify: (id: number, data: PartyModifyRequest) =>
    http.put<PartyDetail>(`${base}/${id}`, data).then((res) => res.data),
  changeJoinPolicy: (id: number, joinPolicy: JoinPolicy) =>
    http.put<PartyDetail>(`${base}/${id}/join-policy`, { joinPolicy }).then((res) => res.data),
  close: (id: number) => http.post<PartyDetail>(`${base}/${id}/close`).then((res) => res.data),
  reopen: (id: number) => http.post<PartyDetail>(`${base}/${id}/reopen`).then((res) => res.data),
  // 초대 코드 새로 만들기. url은 화면 경로(/invite/{code})라 화면이 사이트 주소를 붙인다 (inviteUrlOf)
  regenerateInviteCode: (id: number) => http.post<PartyInvite>(`${base}/${id}/invite-code`).then((res) => res.data),
  dissolve: (id: number) => http.delete<void>(`${base}/${id}`).then(() => undefined),
  // 참가와 나가기 (T022, T031, T033)
  join: (id: number, data: PartyJoinRequestBody = {}) =>
    http.post<PartyDetail>(`${base}/${id}/members`, data).then((res) => res.data),
  leave: (id: number) => http.delete<void>(`${base}/${id}/members/me`).then(() => undefined),
  kick: (id: number, userId: number) => http.delete<void>(`${base}/${id}/members/${userId}`).then(() => undefined),
  // 승인 신청 (T029)
  apply: (id: number, data: PartyJoinRequestBody = {}) =>
    http.post<{ joinRequestId: number }>(`${base}/${id}/join-requests`, data).then((res) => res.data),
  joinRequests: (id: number) => http.get<PartyJoinRequestItem[]>(`${base}/${id}/join-requests`).then((res) => res.data),
  approve: (id: number, joinRequestId: number) =>
    http.post<PartyDetail>(`${base}/${id}/join-requests/${joinRequestId}/approve`).then((res) => res.data),
  reject: (id: number, joinRequestId: number) =>
    http.post<void>(`${base}/${id}/join-requests/${joinRequestId}/reject`).then(() => undefined),
  cancelApplication: (id: number) => http.delete<void>(`${base}/${id}/join-requests/me`).then(() => undefined),
  // 채팅 (T024). before: 위로 올려 더 오래된 것, after: 다시 이은 뒤 빠진 것
  messages: (id: number, params?: { before?: number; after?: number; size?: number }) =>
    http.get<PartyMessagePage>(`${base}/${id}/messages`, { params }).then((res) => res.data),
  sendMessage: (id: number, content: string) =>
    http.post<PartyMessage>(`${base}/${id}/messages`, { content }).then((res) => res.data),
}
