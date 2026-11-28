"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { useForm } from "react-hook-form"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { InviteLinkCopy } from "@/components/party/invite-link-copy"
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
import { emptyPartyInput, type PartyInput, partySchema, toPartySaveRequest } from "@/lib/schemas/party"
import type { PartyDetail } from "@/types/party"

// 파티 만들기 창 (specs/008-party US1, US10, 화면 "파티 만들기", T017·T036)
//   잘못된 칸은 옆에 이유를 보이고 입력은 지우지 않는다 (서버 V001의 칸 오류도 그 칸에 표시)
//   만들면 파티 화면으로 옮겨 간다. 비공개 파티는 초대 링크 복사를 먼저 보인다
export function CreatePartyDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const t = useTranslations("party.create")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const [created, setCreated] = useState<PartyDetail>()
  const form = useForm<PartyInput>({ resolver: zodResolver(partySchema), defaultValues: emptyPartyInput })

  const create = useMutation({
    mutationFn: (values: PartyInput) => api.v1.party.create(toPartySaveRequest(values)),
    onSuccess: async (party) => {
      await queryClient.invalidateQueries({ queryKey: ["parties"] })
      if (party.joinPolicy === "PRIVATE" && party.invite) setCreated(party)
      else router.push(`/parties/${party.id}`)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof PartyInput, { message: "" })
      setFormError(describeError(apiError).message)
    },
  })

  const changeOpen = (next: boolean) => {
    if (!next) {
      form.reset(emptyPartyInput)
      setFormError(undefined)
      setCreated(undefined)
    }
    onOpenChange(next)
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t(created ? "privateCreatedTitle" : "title")}</DialogTitle>
          <DialogDescription>{t(created ? "privateCreatedDescription" : "description")}</DialogDescription>
        </DialogHeader>
        {created?.invite ? (
          <>
            <InviteLinkCopy invite={created.invite} />
            <DialogFooter>
              <Button onClick={() => router.push(`/parties/${created.id}`)}>{t("goToParty")}</Button>
            </DialogFooter>
          </>
        ) : (
          <form
            id="create-party-form"
            noValidate
            onSubmit={form.handleSubmit((values) => {
              setFormError(undefined)
              create.mutate(values)
            })}
            className="flex flex-col gap-4"
          >
            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            <PartyFormFields form={form} idPrefix="create-party" />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={create.isPending}>
                {t("submit")}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
