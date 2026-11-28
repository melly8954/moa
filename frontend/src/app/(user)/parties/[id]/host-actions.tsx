"use client"

import { useQueryClient } from "@tanstack/react-query"
import { KeyRound, Lock, LockOpen, Pencil, Settings, Trash2, UserMinus } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useErrorMessages } from "@/lib/error-messages"
import { JOIN_POLICIES } from "@/lib/schemas/party"
import type { JoinPolicy, PartyDetail, PartyMember } from "@/types/party"
import { EditPartyDialog } from "./edit-party-dialog"
import { partyDetailKey } from "./use-party"
import { RECRUIT_PATH } from "./use-party-live"

// 방장 동작 (T033, US7): 정보 고치기, 참가 방식 바꾸기, 모집 마감·재개, 초대 코드 새로 만들기, 해산
//   되돌리기 어려운 동작은 한 번 더 묻는다 (spec.md 화면 "파티 화면"): 해산, 초대 코드 새로 만들기, 승인에서 다른 방식으로 바꾸기
//   멤버마다 내보내기는 KickButton이다 (멤버 목록의 동작 자리)
//   성공하면 파티 화면을 다시 읽는다. 해산하면 모집 페이지로 보낸다
export function HostActions({ party }: { party: PartyDetail }) {
  const t = useTranslations("party")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [editOpen, setEditOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const run = async (action: () => Promise<unknown>, done: string) => {
    setBusy(true)
    try {
      await action()
      toast.success(done)
      await queryClient.invalidateQueries({ queryKey: partyDetailKey(party.id), exact: true })
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusy(false)
    }
  }

  const changePolicy = async (joinPolicy: JoinPolicy) => {
    if (joinPolicy === party.joinPolicy) return
    if (
      party.joinPolicy === "APPROVAL" &&
      !(await confirm({ title: t("confirm.policyTitle"), description: t("confirm.policyDescription") }))
    )
      return
    await run(() => api.v1.party.changeJoinPolicy(party.id, joinPolicy), t("host.policyChanged"))
  }

  const regenerate = async () => {
    if (!(await confirm({ title: t("confirm.regenerateTitle"), description: t("confirm.regenerateDescription") })))
      return
    await run(() => api.v1.party.regenerateInviteCode(party.id), t("host.regenerated"))
  }

  const dissolve = async () => {
    const ok = await confirm({
      title: t("confirm.dissolveTitle"),
      description: t("confirm.dissolveDescription"),
      confirmLabel: t("host.dissolve"),
      destructive: true,
    })
    if (!ok) return
    setBusy(true)
    try {
      await api.v1.party.dissolve(party.id)
      queryClient.removeQueries({ queryKey: partyDetailKey(party.id) })
      void queryClient.invalidateQueries({ queryKey: ["parties"] })
      toast.success(t("host.dissolved"))
      router.replace(RECRUIT_PATH)
    } catch (error) {
      toast.error(describeError(error).message)
      setBusy(false)
    }
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger render={<Button variant="outline" disabled={busy} />}>
          <Settings aria-hidden />
          {t("host.menu")}
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-56">
          <DropdownMenuItem onClick={() => setEditOpen(true)}>
            <Pencil aria-hidden />
            {t("host.edit")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuGroup>
            <DropdownMenuLabel>{t("host.joinPolicy")}</DropdownMenuLabel>
            <DropdownMenuRadioGroup
              value={party.joinPolicy}
              onValueChange={(value) => void changePolicy(value as JoinPolicy)}
            >
              {JOIN_POLICIES.map((policy) => (
                <DropdownMenuRadioItem key={policy} value={policy}>
                  {t(`policy.${policy}`)}
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          {party.status === "RECRUITING" ? (
            <DropdownMenuItem onClick={() => void run(() => api.v1.party.close(party.id), t("host.closed"))}>
              <Lock aria-hidden />
              {t("host.close")}
            </DropdownMenuItem>
          ) : (
            <DropdownMenuItem onClick={() => void run(() => api.v1.party.reopen(party.id), t("host.reopened"))}>
              <LockOpen aria-hidden />
              {t("host.reopen")}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => void regenerate()}>
            <KeyRound aria-hidden />
            {t("host.regenerate")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => void dissolve()}>
            <Trash2 aria-hidden />
            {t("host.dissolve")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditPartyDialog party={party} open={editOpen} onOpenChange={setEditOpen} />
    </>
  )
}

// 멤버 내보내기 (US7-AC5). 다시 들어올 수 없다고 알린다. 방장 자신에게는 보이지 않는다
export function KickButton({ party, member }: { party: PartyDetail; member: PartyMember }) {
  const t = useTranslations("party")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [busy, setBusy] = useState(false)
  if (member.role === "HOST") return null

  const kick = async () => {
    const ok = await confirm({
      title: t("confirm.kickTitle", { name: member.displayName }),
      description: t("confirm.kickDescription"),
      confirmLabel: t("host.kick"),
      destructive: true,
    })
    if (!ok) return
    setBusy(true)
    try {
      await api.v1.party.kick(party.id, member.userId)
      toast.success(t("host.kicked", { name: member.displayName }))
      await queryClient.invalidateQueries({ queryKey: partyDetailKey(party.id), exact: true })
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Button
      size="sm"
      variant="ghost"
      disabled={busy}
      aria-label={t("host.kickOf", { name: member.displayName })}
      onClick={() => void kick()}
    >
      <UserMinus aria-hidden />
      <span className="hidden sm:inline">{t("host.kick")}</span>
    </Button>
  )
}
