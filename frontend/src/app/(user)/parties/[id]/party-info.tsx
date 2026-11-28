"use client"

import { Users } from "lucide-react"
import { useTranslations } from "next-intl"
import { CodeName } from "@/components/code/code-select"
import { Badge } from "@/components/ui/badge"
import type { PartyDetail } from "@/types/party"

// 파티 정보 (spec.md 화면 "파티 화면"): 게임, 모드, 제목, 소개, 티어 조건, 참가 방식, 상태, 인원
//   누구에게나 같다. 동작 버튼은 actions 자리에 받는다
export function PartyInfo({ party, actions }: { party: PartyDetail; actions?: React.ReactNode }) {
  const t = useTranslations("party.detail")
  const tp = useTranslations("party")
  const tierGroup = `${party.gameCode}_TIER`
  const hasTier = party.tierMinCode || party.tierMaxCode

  return (
    <header className="flex flex-col gap-3 border-b pb-4">
      <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
        <Badge variant="secondary">
          <CodeName groupCode="GAME" code={party.gameCode} />
        </Badge>
        {party.modeCode && (
          <Badge variant="outline">
            <CodeName groupCode={`${party.gameCode}_MODE`} code={party.modeCode} />
          </Badge>
        )}
        <Badge variant="outline">{tp(`policy.${party.joinPolicy}`)}</Badge>
        <Badge variant={party.status === "RECRUITING" ? "default" : "destructive"}>{t(`status.${party.status}`)}</Badge>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <h1 className="min-w-0 text-xl font-semibold break-words">{party.title}</h1>
        {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
      </div>
      <dl className="flex flex-wrap gap-x-6 gap-y-1 text-sm">
        <div className="flex items-center gap-1">
          <dt>
            <Users className="size-4" aria-hidden />
            <span className="sr-only">{t("memberCount")}</span>
          </dt>
          <dd>{t("memberOf", { count: party.memberCount, capacity: party.capacity })}</dd>
        </div>
        <div className="flex items-center gap-1">
          <dt className="text-muted-foreground">{t("tier")}</dt>
          <dd>
            {hasTier ? (
              <>
                {party.tierMinCode ? <CodeName groupCode={tierGroup} code={party.tierMinCode} /> : t("tierOpen")}
                {" ~ "}
                {party.tierMaxCode ? <CodeName groupCode={tierGroup} code={party.tierMaxCode} /> : t("tierOpen")}
              </>
            ) : (
              t("tierAny")
            )}
          </dd>
        </div>
        {party.status === "CLOSED" && party.closedByHost && (
          <p className="text-muted-foreground">{t("closedByHost")}</p>
        )}
      </dl>
      {party.description && <p className="text-sm break-words whitespace-pre-wrap">{party.description}</p>}
    </header>
  )
}
