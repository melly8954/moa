"use client"

import { notFound, useParams } from "next/navigation"
import { toApiError } from "@/api/error"
import { QueryState } from "@/components/kit/query-state"
import { InviteLinkCopy } from "@/components/party/invite-link-copy"
import { useTranslations } from "next-intl"
import type { PartyDetail } from "@/types/party"
import { ChatPanel } from "./chat-panel"
import { HostActions, KickButton } from "./host-actions"
import { JoinButton } from "./join-button"
import { JoinRequests } from "./join-requests"
import { LeaveButton } from "./leave-button"
import { MemberList } from "./member-list"
import { PartyInfo } from "./party-info"
import { isMember, useParty } from "./use-party"
import { usePartyLive } from "./use-party-live"

// 파티 화면 (specs/008-party T020, spec.md 화면 "파티 화면"). 로그인 없이도 공개·승인 파티를 본다
//   없는 파티·해산·비멤버의 비공개(R001)는 not-found로 보낸다
//   보는 사람에 따라: 비멤버는 참가·신청, 멤버는 채팅·초대·나가기, 방장은 방장 동작·신청 목록·내보내기를 더 본다
export default function PartyPage() {
  const id = Number(useParams<{ id: string }>().id)
  const party = useParty(id)
  usePartyLive(id, party.data)

  if (!Number.isInteger(id) || (party.isError && toApiError(party.error).code === "R001")) notFound()

  return (
    <article className="flex min-w-0 flex-col gap-6">
      <QueryState query={party}>{(data) => <PartyView party={data} />}</QueryState>
    </article>
  )
}

function PartyView({ party }: { party: PartyDetail }) {
  const t = useTranslations("party.detail")
  const member = isMember(party)
  const host = party.me.relation === "HOST"

  return (
    <>
      <PartyInfo
        party={party}
        actions={
          member ? (
            <>
              {host && <HostActions party={party} />}
              <LeaveButton party={party} />
            </>
          ) : (
            <JoinButton party={party} />
          )
        }
      />
      <div className="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,28rem)]">
        <div className="flex min-w-0 flex-col gap-6">
          <MemberList party={party} action={host ? (m) => <KickButton party={party} member={m} /> : undefined} />
          {host && party.joinPolicy === "APPROVAL" && <JoinRequests party={party} />}
          {member && party.invite && (
            <section aria-labelledby="party-invite" className="flex flex-col gap-2">
              <h2 id="party-invite" className="font-medium">
                {t("invite")}
              </h2>
              <InviteLinkCopy invite={party.invite} />
            </section>
          )}
        </div>
        {member && <ChatPanel partyId={party.id} />}
      </div>
    </>
  )
}
