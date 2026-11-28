"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { PartyFormFields } from "@/components/party/party-form-fields"
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
import { useErrorMessages } from "@/lib/error-messages"
import { type PartyInput, partySchema, toPartySaveRequest } from "@/lib/schemas/party"
import type { PartyDetail, PartyModifyRequest, PartySaveRequest } from "@/types/party"
import { partyDetailKey } from "./use-party"

// 파티 → 고치기 폼 값. 방장 포지션은 방장 멤버의 포지션이다
function toInput(party: PartyDetail): PartyInput {
  const hostMember = party.members.find((m) => m.role === "HOST")
  return {
    gameCode: party.gameCode,
    modeCode: party.modeCode ?? "",
    title: party.title,
    description: party.description ?? "",
    joinPolicy: party.joinPolicy,
    capacity: party.capacity,
    tierMinCode: party.tierMinCode ?? "",
    tierMaxCode: party.tierMaxCode ?? "",
    positionEnabled: party.positionEnabled,
    hostPositionCode: hostMember?.positionCode ?? "",
    slots: Object.fromEntries(party.positions.map((p) => [p.positionCode, p.slots])),
  }
}

// 정보 고치기 창 (T033, US7-AC1). 만들기 창의 폼 칸(PartyFormFields)을 다시 쓴다. 참가 방식은 따로 바꾼다
//   B109(인원을 지금보다 작게)·B110(차지된 포지션)·B111(게임 바꾸기)은 창 안에 문구로 보이고 입력은 지우지 않는다
export function EditPartyDialog({
  party,
  open,
  onOpenChange,
}: {
  party: PartyDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const t = useTranslations("party.host")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const form = useForm<PartyInput>({ resolver: zodResolver(partySchema), values: open ? toInput(party) : undefined })

  const save = useMutation({
    mutationFn: (values: PartyInput) => {
      // 참가 방식은 방장 메뉴에서 따로 바꾼다 (PUT /join-policy)
      const body: PartyModifyRequest = toPartySaveRequest(values)
      delete (body as Partial<PartySaveRequest>).joinPolicy
      return api.v1.party.modify(party.id, body)
    },
    onSuccess: (detail) => {
      queryClient.setQueryData(partyDetailKey(party.id), detail)
      toast.success(t("saved"))
      changeOpen(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof PartyInput, { message: "" })
      setFormError(describeError(apiError).message)
    },
  })

  const changeOpen = (next: boolean) => {
    if (save.isPending) return
    if (!next) setFormError(undefined)
    onOpenChange(next)
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("editTitle")}</DialogTitle>
          <DialogDescription>{t("editDescription")}</DialogDescription>
        </DialogHeader>
        <form
          noValidate
          className="flex flex-col gap-4"
          onSubmit={form.handleSubmit((values) => {
            setFormError(undefined)
            save.mutate(values)
          })}
        >
          {formError && (
            <Alert variant="destructive">
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <PartyFormFields form={form} idPrefix="edit-party" showJoinPolicy={false} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
              {t("cancel")}
            </Button>
            <Button type="submit" disabled={save.isPending}>
              {t("save")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
