"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Users } from "lucide-react"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { api } from "@/api"
import { CodeName } from "@/components/code/code-select"
import { confirm } from "@/components/kit/confirm-dialog"
import { LinkButton } from "@/components/kit/link-button"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader } from "@/components/ui/empty"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import { positionGroupOf } from "@/lib/schemas/party"
import type { MyPartyJoinRequest } from "@/types/party"

// 내 파티 (specs/008-party US8, 화면 "내 파티", T038)
//   속한 파티: 카드(게임, 제목, 내 역할, 인원)와 파티 화면 버튼
//   대기 중인 신청: 신청순으로 파티 이름, 고른 포지션, 신청 시각, "신청 취소"
//   둘 다 없으면 모집 페이지로 가는 길
export default function MyPartyPage() {
  const t = useTranslations("party.myParty")
  const myParty = useQuery({ queryKey: ["parties", "me"], queryFn: api.v1.party.me })

  return (
    <div className="flex flex-col gap-(--page-gap)">
      <PageHeader title={t("title")} description={t("description")} />
      <QueryState query={myParty}>
        {({ party, joinRequests }) =>
          !party && joinRequests.length === 0 ? (
            <Empty>
              <EmptyHeader>
                <EmptyDescription>{t("empty")}</EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <LinkButton href="/recruit">{t("toRecruit")}</LinkButton>
              </EmptyContent>
            </Empty>
          ) : (
            <>
              {party && (
                <section className="flex flex-col gap-2" aria-labelledby="my-party-current">
                  <h2 id="my-party-current" className="font-medium">
                    {t("current")}
                  </h2>
                  <Card className="max-w-lg">
                    <CardHeader>
                      <CardDescription>
                        <CodeName groupCode="GAME" code={party.gameCode} />
                      </CardDescription>
                      <CardTitle className="break-words">{party.title}</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-wrap items-center gap-2 text-sm">
                      <Badge variant={party.myRole === "HOST" ? "default" : "secondary"}>
                        {t(`role.${party.myRole}`)}
                      </Badge>
                      <span className="flex items-center gap-1 tabular-nums">
                        <Users className="size-4" aria-hidden />
                        <span className="sr-only">{t("members")}</span>
                        {party.memberCount}/{party.capacity}
                      </span>
                    </CardContent>
                    <CardFooter>
                      <LinkButton href={`/parties/${party.id}`}>{t("openParty")}</LinkButton>
                    </CardFooter>
                  </Card>
                </section>
              )}
              {joinRequests.length > 0 && (
                <section className="flex flex-col gap-2" aria-labelledby="my-party-requests">
                  <h2 id="my-party-requests" className="font-medium">
                    {t("pendingRequests")}
                  </h2>
                  <ul className="flex max-w-lg flex-col divide-y rounded-xl border">
                    {joinRequests.map((request) => (
                      <JoinRequestRow key={request.joinRequestId} request={request} />
                    ))}
                  </ul>
                </section>
              )}
            </>
          )
        }
      </QueryState>
    </div>
  )
}

function JoinRequestRow({ request }: { request: MyPartyJoinRequest }) {
  const t = useTranslations("party.myParty")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const cancel = useMutation({
    mutationFn: () => api.v1.party.cancelApplication(request.party.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["parties"] })
      toast.success(t("cancelled"))
    },
    onError: (error) => toast.error(describeError(error).message),
  })
  return (
    <li className="flex flex-wrap items-center justify-between gap-2 p-3 text-sm">
      <div className="flex min-w-0 flex-col gap-0.5">
        <LinkButton href={`/parties/${request.party.id}`} variant="link" className="h-auto justify-start p-0">
          <span className="truncate">{request.party.title}</span>
        </LinkButton>
        <span className="text-muted-foreground">
          <CodeName groupCode="GAME" code={request.party.gameCode} />
          {request.positionCode && (
            <>
              {" · "}
              <CodeName groupCode={positionGroupOf(request.party.gameCode)} code={request.positionCode} />
            </>
          )}
          {" · "}
          <time dateTime={request.appliedAt}>{formatDateTime(request.appliedAt)}</time>
        </span>
      </div>
      <Button
        variant="outline"
        size="sm"
        disabled={cancel.isPending}
        onClick={async () => {
          if (await confirm({ title: t("cancelTitle", { title: request.party.title }), confirmLabel: t("cancel") }))
            cancel.mutate()
        }}
      >
        {t("cancel")}
      </Button>
    </li>
  )
}
