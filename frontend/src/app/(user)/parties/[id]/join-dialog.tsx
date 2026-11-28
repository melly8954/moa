"use client"

import { useTranslations } from "next-intl"
import { useState } from "react"
import { toApiError } from "@/api/error"
import { CodeName, CodeSelect } from "@/components/code/code-select"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldDescription, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { useCodes } from "@/lib/codes"
import { useErrorMessages } from "@/lib/error-messages"
import type { OpenPosition, PartyDetail, PartyJoinRequestBody } from "@/types/party"

// 참가 창 (T036, spec.md 화면 "참가 창"). 포지션이나 티어 조건이 있는 파티에 참가·신청할 때 연다
//   빈 포지션만 고를 수 있다. 티어는 선택이고 범위 밖이면 안내만 한다
//   B105(그사이 그 포지션이 참)면 이유를 알리고 details.openPositions로 남은 포지션을 다시 보인다
export function needsJoinDialog(party: PartyDetail) {
  return party.positionEnabled || !!party.tierMinCode || !!party.tierMaxCode
}

export function JoinDialog({
  party,
  open,
  onOpenChange,
  submitLabel,
  onSubmit,
}: {
  party: PartyDetail
  open: boolean
  onOpenChange: (open: boolean) => void
  submitLabel: string
  // 실패하면 ApiError를 던진다. B105 말고는 창 안에 문구로 보인다
  onSubmit: (input: PartyJoinRequestBody) => Promise<void>
}) {
  const t = useTranslations("party.join")
  const { describeError } = useErrorMessages()
  const [positionCode, setPositionCode] = useState("")
  const [tierCode, setTierCode] = useState("")
  const [error, setError] = useState<string>()
  const [openOverride, setOpenOverride] = useState<OpenPosition[]>()
  const [submitting, setSubmitting] = useState(false)
  const tiers = useCodes(`${party.gameCode}_TIER`, { includeDisabled: true }).data ?? []

  const open_ = (openOverride ?? party.openPositions).filter((p) => p.remaining > 0)
  const isOpen = (code: string) => open_.some((p) => p.positionCode === code)
  const positionGroup = `${party.gameCode}_POSITION`

  const order = (code: string | null | undefined) => tiers.find((c) => c.code === code)?.sortOrder
  const chosen = order(tierCode)
  const min = order(party.tierMinCode)
  const max = order(party.tierMaxCode)
  const outOfRange = chosen != null && ((min != null && chosen < min) || (max != null && chosen > max))

  const changeOpen = (next: boolean) => {
    if (submitting) return
    if (!next) {
      setPositionCode("")
      setTierCode("")
      setError(undefined)
      setOpenOverride(undefined)
    }
    onOpenChange(next)
  }

  const submit = async () => {
    if (party.positionEnabled && !positionCode) {
      setError(t("positionRequired"))
      return
    }
    setSubmitting(true)
    setError(undefined)
    try {
      await onSubmit({ positionCode: positionCode || undefined, tierCode: tierCode || undefined })
      setSubmitting(false)
      changeOpen(false)
    } catch (e) {
      const apiError = toApiError(e)
      if (apiError.code === "B105") {
        const left = (apiError.body?.details as unknown as { openPositions?: OpenPosition[] } | undefined)
          ?.openPositions
        setOpenOverride(Array.isArray(left) ? left : [])
        setPositionCode("")
      }
      setError(describeError(apiError).message)
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("dialogTitle")}</DialogTitle>
          <DialogDescription>{party.title}</DialogDescription>
        </DialogHeader>
        {error && (
          <Alert variant="destructive" role="alert">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        {party.positionEnabled && (
          <FieldSet>
            <FieldLegend variant="label">{t("position")}</FieldLegend>
            {open_.length === 0 && <FieldDescription>{t("noOpenPosition")}</FieldDescription>}
            <RadioGroup value={positionCode} onValueChange={(value) => setPositionCode(String(value))}>
              {party.positions.map((p) => {
                const available = isOpen(p.positionCode)
                const inputId = `join-position-${p.positionCode}`
                return (
                  <div key={p.positionCode} className="flex items-center gap-2">
                    <RadioGroupItem id={inputId} value={p.positionCode} disabled={!available} />
                    <Label htmlFor={inputId} className={available ? undefined : "text-muted-foreground"}>
                      <CodeName groupCode={positionGroup} code={p.positionCode} />
                      {!available && <span>{t("positionFull")}</span>}
                    </Label>
                  </div>
                )
              })}
            </RadioGroup>
          </FieldSet>
        )}
        <Field>
          <FieldLabel htmlFor="join-tier">{t("tier")}</FieldLabel>
          <CodeSelect
            id="join-tier"
            groupCode={`${party.gameCode}_TIER`}
            value={tierCode}
            onChange={setTierCode}
            placeholder={t("tierNone")}
          />
          <FieldDescription>{outOfRange ? t("tierOutOfRange") : t("tierHelp")}</FieldDescription>
        </Field>
        <DialogFooter>
          <Button variant="outline" disabled={submitting} onClick={() => changeOpen(false)}>
            {t("cancel")}
          </Button>
          <Button disabled={submitting || (party.positionEnabled && open_.length === 0)} onClick={submit}>
            {submitLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
