"use client"

import { useQueryClient } from "@tanstack/react-query"
import { LogOut } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import type { PartyDetail } from "@/types/party"
import { partyDetailKey } from "./use-party"
import { RECRUIT_PATH } from "./use-party-live"

// 나가기 (T031, US6). 확인 창에서 마지막 멤버면 "파티가 해산된다", 방장이면 "가장 먼저 들어온 멤버가 방장이 된다"를 알린다
//   나가면 모집 페이지로 보낸다
export function LeaveButton({ party }: { party: PartyDetail }) {
  const t = useTranslations("party")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [busy, setBusy] = useState(false)

  const leave = async () => {
    const last = party.memberCount <= 1
    const ok = await confirm({
      title: t("confirm.leave"),
      description: last ? t("confirm.leaveLast") : party.me.relation === "HOST" ? t("confirm.leaveHost") : undefined,
      confirmLabel: t("detail.leave"),
      destructive: true,
    })
    if (!ok) return
    setBusy(true)
    try {
      await api.v1.party.leave(party.id)
      queryClient.removeQueries({ queryKey: partyDetailKey(party.id) })
      void queryClient.invalidateQueries({ queryKey: ["parties"] })
      toast.success(t("detail.left"))
      router.replace(RECRUIT_PATH)
    } catch (error) {
      toast.error(describeError(error).message)
      setBusy(false)
    }
  }

  return (
    <Button variant="outline" disabled={busy} onClick={() => void leave()}>
      <LogOut aria-hidden />
      {t("detail.leave")}
    </Button>
  )
}
