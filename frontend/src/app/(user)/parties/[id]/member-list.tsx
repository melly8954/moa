"use client"

import { Crown } from "lucide-react"
import { useTranslations } from "next-intl"
import { CodeName } from "@/components/code/code-select"
import type { PartyDetail, PartyMember } from "@/types/party"

// 멤버 목록 (T020): 표시 이름, 방장 표시, 포지션, 적은 티어, 포지션별 빈자리
//   방장 화면은 멤버마다 동작(내보내기)을 action으로 받는다 (T033)
export function MemberList({
  party,
  action,
}: {
  party: PartyDetail
  action?: (member: PartyMember) => React.ReactNode
}) {
  const t = useTranslations("party.detail")
  const positionGroup = `${party.gameCode}_POSITION`

  return (
    <section aria-labelledby="party-members" className="flex flex-col gap-3">
      <h2 id="party-members" className="font-medium">
        {t("members")}
      </h2>
      {party.positionEnabled && party.positions.length > 0 && (
        <div className="flex flex-col gap-1">
          <h3 className="text-sm text-muted-foreground">{t("vacancies")}</h3>
          <ul className="flex flex-wrap gap-2 text-sm">
            {party.positions.map((p) => (
              <li
                key={p.positionCode}
                className={
                  p.filled < p.slots
                    ? "rounded-md border px-2 py-1"
                    : "rounded-md border px-2 py-1 text-muted-foreground"
                }
              >
                <CodeName groupCode={positionGroup} code={p.positionCode} />{" "}
                {t("vacancyOf", { filled: p.filled, slots: p.slots })}
              </li>
            ))}
          </ul>
        </div>
      )}
      <ul className="flex flex-col divide-y rounded-lg border">
        {party.members.map((m) => (
          <li key={m.userId} className="flex flex-wrap items-center gap-x-3 gap-y-1 px-3 py-2 text-sm">
            <span className="flex min-w-0 items-center gap-1 font-medium">
              {m.role === "HOST" && (
                <>
                  <Crown className="size-4 text-primary" aria-hidden />
                  <span className="sr-only">{t("hostMark")}</span>
                </>
              )}
              <span className="truncate">{m.displayName}</span>
            </span>
            {m.positionCode && (
              <span className="text-muted-foreground">
                <CodeName groupCode={positionGroup} code={m.positionCode} />
              </span>
            )}
            {m.tierCode && (
              <span className="text-muted-foreground">
                <CodeName groupCode={`${party.gameCode}_TIER`} code={m.tierCode} />
              </span>
            )}
            {action && <span className="ml-auto">{action(m)}</span>}
          </li>
        ))}
      </ul>
    </section>
  )
}
