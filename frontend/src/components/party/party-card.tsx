"use client"

import { Crown, Users } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { CodeName } from "@/components/code/code-select"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { modeGroupOf, positionGroupOf, tierGroupOf } from "@/lib/schemas/party"
import type { JoinPolicy, OpenPosition, PartySummary } from "@/types/party"

// 파티 표시 조각 (모집 카드, 내 파티, 초대 입장 화면이 함께 쓴다)

export function JoinPolicyBadge({ policy }: { policy: JoinPolicy }) {
  const t = useTranslations("party.policy")
  return <Badge variant={policy === "OPEN" ? "default" : "secondary"}>{t(policy)}</Badge>
}

// 게임 · 모드
export function GameModeText({ gameCode, modeCode }: { gameCode: string; modeCode: string | null }) {
  return (
    <>
      <CodeName groupCode="GAME" code={gameCode} />
      {modeCode && (
        <>
          {" · "}
          <CodeName groupCode={modeGroupOf(gameCode)} code={modeCode} />
        </>
      )}
    </>
  )
}

// 티어 조건: "골드 ~ 다이아몬드", "골드 이상", "다이아몬드 이하". 없으면 그리지 않는다
export function TierRangeText({
  gameCode,
  tierMinCode,
  tierMaxCode,
}: {
  gameCode: string
  tierMinCode: string | null
  tierMaxCode: string | null
}) {
  const t = useTranslations("party.card")
  const group = tierGroupOf(gameCode)
  if (!tierMinCode && !tierMaxCode) return null
  const min = tierMinCode ? <CodeName groupCode={group} code={tierMinCode} /> : null
  const max = tierMaxCode ? <CodeName groupCode={group} code={tierMaxCode} /> : null
  return (
    <span>
      {t("tier")}{" "}
      {min && max ? (
        <>
          {min} ~ {max}
        </>
      ) : min ? (
        t.rich("tierAtLeast", { tier: () => min })
      ) : (
        t.rich("tierAtMost", { tier: () => max })
      )}
    </span>
  )
}

// 빈 포지션: "미드 1 · 서포터 1"
export function OpenPositionsText({ gameCode, positions }: { gameCode: string; positions: OpenPosition[] }) {
  const t = useTranslations("party.card")
  const open = positions.filter((p) => p.remaining > 0)
  if (open.length === 0) return null
  return (
    <span>
      {t("openPositions")}{" "}
      {open.map((p, i) => (
        <span key={p.positionCode}>
          {i > 0 && " · "}
          <CodeName groupCode={positionGroupOf(gameCode)} code={p.positionCode} /> {p.remaining}
        </span>
      ))}
    </span>
  )
}

// 모집 페이지 카드. 누르면 파티 화면이 열린다 (spec.md 화면 "모집 페이지")
export function PartyCard({ party }: { party: PartySummary }) {
  const t = useTranslations("party.card")
  return (
    <Link
      href={`/parties/${party.id}`}
      className="block rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <Card className="h-full transition-colors hover:bg-muted/50">
        <CardHeader>
          <CardDescription>
            <GameModeText gameCode={party.gameCode} modeCode={party.modeCode} />
          </CardDescription>
          <CardTitle className="break-words">{party.title}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <JoinPolicyBadge policy={party.joinPolicy} />
            <span className="flex items-center gap-1 tabular-nums">
              <Users className="size-4" aria-hidden />
              <span className="sr-only">{t("members")}</span>
              {party.memberCount}/{party.capacity}
            </span>
          </div>
          <div className="flex flex-col gap-1 text-muted-foreground">
            <OpenPositionsText gameCode={party.gameCode} positions={party.openPositions} />
            <TierRangeText gameCode={party.gameCode} tierMinCode={party.tierMinCode} tierMaxCode={party.tierMaxCode} />
            <span className="flex items-center gap-1">
              <Crown className="size-4" aria-hidden />
              <span className="sr-only">{t("host")}</span>
              {party.host.displayName}
            </span>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
