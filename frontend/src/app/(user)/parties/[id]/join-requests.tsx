"use client"

import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { CodeName } from "@/components/code/code-select"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import type { PartyDetail, PartyJoinRequestItem } from "@/types/party"
import { partyDetailKey, partyJoinRequestsKey } from "./use-party"

// 방장의 신청 목록 (T029, US5). 승인 파티의 방장에게만 보인다
//   대기 수, 신청순 목록, 승인·거절. 실시간 JOIN_REQUESTS_CHANGED면 다시 읽는다 (use-party-live.ts)
//   승인하면 응답(PartyDetail)으로 파티 화면을 바꾼다. 실패(B101 정원, B105 포지션, B103 다른 파티)는 문구로 알리고 목록을 다시 읽는다
export function JoinRequests({ party }: { party: PartyDetail }) {
  const t = useTranslations("party.host")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [busyId, setBusyId] = useState<number>()
  const requests = useQuery({
    queryKey: partyJoinRequestsKey(party.id),
    queryFn: () => api.v1.party.joinRequests(party.id),
  })
  const positionGroup = `${party.gameCode}_POSITION`

  const handle = async (request: PartyJoinRequestItem, approve: boolean) => {
    setBusyId(request.joinRequestId)
    try {
      if (approve) {
        const detail = await api.v1.party.approve(party.id, request.joinRequestId)
        queryClient.setQueryData(partyDetailKey(party.id), detail)
        toast.success(t("approved", { name: request.user.displayName }))
      } else {
        await api.v1.party.reject(party.id, request.joinRequestId)
        toast.success(t("rejected", { name: request.user.displayName }))
      }
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusyId(undefined)
      void queryClient.invalidateQueries({ queryKey: partyJoinRequestsKey(party.id) })
    }
  }

  return (
    <section aria-labelledby="party-requests" className="flex flex-col gap-3">
      <h2 id="party-requests" className="flex items-center gap-2 font-medium">
        {t("requests")}
        {requests.data && (
          <span className="text-sm font-normal text-muted-foreground">
            {t("pendingCount", { count: requests.data.length })}
          </span>
        )}
      </h2>
      <QueryState query={requests} isEmpty={(d) => d.length === 0} empty={t("requestsEmpty")}>
        {(data) => (
          <ul className="flex flex-col divide-y rounded-lg border">
            {data.map((r) => (
              <li key={r.joinRequestId} className="flex flex-wrap items-center gap-x-3 gap-y-1 px-3 py-2 text-sm">
                <span className="font-medium">{r.user.displayName}</span>
                {r.positionCode && (
                  <span className="text-muted-foreground">
                    <CodeName groupCode={positionGroup} code={r.positionCode} />
                  </span>
                )}
                {r.tierCode && (
                  <span className="text-muted-foreground">
                    <CodeName groupCode={`${party.gameCode}_TIER`} code={r.tierCode} />
                  </span>
                )}
                <time className="text-xs text-muted-foreground" dateTime={r.appliedAt}>
                  {formatDateTime(r.appliedAt)}
                </time>
                <span className="ml-auto flex gap-2">
                  <Button
                    size="sm"
                    disabled={busyId != null}
                    aria-label={t("approveOf", { name: r.user.displayName })}
                    onClick={() => void handle(r, true)}
                  >
                    {t("approve")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={busyId != null}
                    aria-label={t("rejectOf", { name: r.user.displayName })}
                    onClick={() => void handle(r, false)}
                  >
                    {t("reject")}
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </QueryState>
    </section>
  )
}
