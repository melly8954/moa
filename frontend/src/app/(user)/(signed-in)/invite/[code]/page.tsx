"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Crown, Users } from "lucide-react"
import { useParams, useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useEffect, useState } from "react"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { CodeName, CodeSelect } from "@/components/code/code-select"
import { LinkButton } from "@/components/kit/link-button"
import { QueryState } from "@/components/kit/query-state"
import { GameModeText } from "@/components/party/party-card"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { useErrorMessages } from "@/lib/error-messages"
import { positionGroupOf, tierGroupOf } from "@/lib/schemas/party"
import type { InvitePreview } from "@/types/party"

// 초대 입장 화면 (specs/008-party US4, 화면 "초대 입장 화면", T027)
//   로그인하지 않았으면 (signed-in) 묶음의 AuthGate가 ?next=로 이 화면에 돌아오게 한다
//   이미 멤버면 파티 화면으로 바로 보낸다. 없는·바뀐 코드, 해산된 파티(B107)면 안내와 모집 페이지로 가는 길
//   못 들어오는 이유(reasonCode)가 있으면 이유를 보이고 "들어가기"를 막는다
//   포지션 파티면 빈 포지션을 고르고 티어를 적을 수 있다 (US10-AC5)
export default function InvitePage() {
  const t = useTranslations("party.invite")
  const code = useParams<{ code: string }>().code
  const router = useRouter()
  const preview = useQuery({
    queryKey: ["parties", "invite", code],
    queryFn: () => api.v1.party.getInvite(code),
    retry: (count, error) => toApiError(error).code !== "B107" && count < 2,
  })
  const invalid = preview.isError && toApiError(preview.error).code === "B107"
  const relation = preview.data?.me.relation
  const memberPartyId = relation === "MEMBER" || relation === "HOST" ? preview.data?.id : undefined

  useEffect(() => {
    if (memberPartyId != null) router.replace(`/parties/${memberPartyId}`)
  }, [memberPartyId, router])

  if (invalid)
    return (
      <div className="flex flex-col items-start gap-3">
        <h1 className="text-xl font-semibold">{t("invalidTitle")}</h1>
        <p className="text-sm text-muted-foreground">{t("invalidDescription")}</p>
        <LinkButton href="/recruit" variant="outline">
          {t("toRecruit")}
        </LinkButton>
      </div>
    )

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">{t("title")}</h1>
      <QueryState query={preview}>
        {(data) => (memberPartyId != null ? null : <InviteEntry code={code} preview={data} />)}
      </QueryState>
    </div>
  )
}

function InviteEntry({ code, preview }: { code: string; preview: InvitePreview }) {
  const t = useTranslations("party.invite")
  const tc = useTranslations("party.card")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { errorMessage, describeError } = useErrorMessages()
  const [positionCode, setPositionCode] = useState("")
  const [tierCode, setTierCode] = useState("")
  const [error, setError] = useState<string>()
  const openPositions = preview.positions.filter((p) => p.filled < p.slots)
  const needsPosition = preview.positions.length > 0

  const join = useMutation({
    mutationFn: () =>
      api.v1.party.joinByInvite(code, {
        positionCode: positionCode || undefined,
        tierCode: tierCode || undefined,
      }),
    onSuccess: async (party) => {
      await queryClient.invalidateQueries({ queryKey: ["parties"] })
      router.push(`/parties/${party.id}`)
    },
    onError: (e) => {
      const apiError = toApiError(e)
      setError(describeError(apiError).message)
      // 그사이 포지션이 찼으면 남은 포지션을 다시 보인다
      if (apiError.code === "B105") {
        setPositionCode("")
        void queryClient.invalidateQueries({ queryKey: ["parties", "invite", code] })
      }
    },
  })

  return (
    <Card className="max-w-lg">
      <CardHeader>
        <CardDescription>
          <GameModeText gameCode={preview.gameCode} modeCode={preview.modeCode} />
        </CardDescription>
        <CardTitle className="break-words">{preview.title}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 text-sm">
        <div className="flex flex-wrap gap-4 text-muted-foreground">
          <span className="flex items-center gap-1">
            <Crown className="size-4" aria-hidden />
            <span className="sr-only">{tc("host")}</span>
            {preview.host.displayName}
          </span>
          <span className="flex items-center gap-1 tabular-nums">
            <Users className="size-4" aria-hidden />
            <span className="sr-only">{tc("members")}</span>
            {preview.memberCount}/{preview.capacity}
          </span>
        </div>
        {!preview.joinable && preview.reasonCode && (
          <Alert variant="destructive" role="alert">
            <AlertDescription>{errorMessage(preview.reasonCode)}</AlertDescription>
          </Alert>
        )}
        {preview.joinable && needsPosition && (
          <Field>
            <FieldLabel htmlFor="invite-position">{t("position")}</FieldLabel>
            <NativeSelect id="invite-position" value={positionCode} onChange={(e) => setPositionCode(e.target.value)}>
              <NativeSelectOption value="">{t("choosePosition")}</NativeSelectOption>
              {openPositions.map((p) => (
                <NativeSelectOption key={p.positionCode} value={p.positionCode}>
                  {/* option 안에는 글자만 들어가므로 이름은 CodeName으로 그린다 */}
                  <CodeName groupCode={positionGroupOf(preview.gameCode)} code={p.positionCode} />
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
        )}
        {preview.joinable && (
          <Field>
            <FieldLabel htmlFor="invite-tier">{t("tier")}</FieldLabel>
            <CodeSelect
              groupCode={tierGroupOf(preview.gameCode)}
              id="invite-tier"
              value={tierCode}
              onChange={setTierCode}
              placeholder={t("tierNone")}
            />
            <FieldDescription>{t("tierHint")}</FieldDescription>
          </Field>
        )}
        {error && (
          <Alert variant="destructive" role="alert">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
      </CardContent>
      <CardFooter className="gap-2">
        <Button
          disabled={!preview.joinable || join.isPending || (needsPosition && !positionCode)}
          onClick={() => {
            setError(undefined)
            join.mutate()
          }}
        >
          {t("enter")}
        </Button>
        <LinkButton href="/recruit" variant="outline">
          {t("toRecruit")}
        </LinkButton>
      </CardFooter>
    </Card>
  )
}
