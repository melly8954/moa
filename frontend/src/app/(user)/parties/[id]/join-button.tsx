"use client"

import { useQueryClient } from "@tanstack/react-query"
import { usePathname, useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { LinkButton } from "@/components/kit/link-button"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import { signInPath } from "@/lib/next-path"
import { useAuthStore } from "@/stores/auth"
import type { PartyDetail, PartyJoinRequestBody } from "@/types/party"
import { JoinDialog, needsJoinDialog } from "./join-dialog"
import { partyDetailKey } from "./use-party"

export const MY_PARTY_PATH = "/my-party"

// 참가에 실패했을 때 버튼을 막고 이유를 보일 코드 (정원 참, 마감, 다른 파티에 속함, 참가 제한)
const BLOCKING = new Set(["B101", "B102", "B103", "B104"])

// 멤버가 아닌 회원의 참가 버튼 (T022, T029, spec.md 화면 "파티 화면")
//   공개: "참가" / 승인: "신청", 대기 중이면 "신청 대기 중" + "신청 취소"
//   정원 참·마감·참가 제한이면 누를 수 없고 이유를 보인다. 이유는 me.relation과 status로 정한다
//   다른 파티에 속했는지는 상세 응답에 없어 B103을 받은 뒤에 보이고, 내 파티로 가는 길을 함께 보인다
//   로그인하지 않았으면 로그인 화면으로 갔다가 돌아온다
//   포지션·티어 조건이 있으면 참가 창(T036)을 연다
export function JoinButton({ party }: { party: PartyDetail }) {
  const t = useTranslations("party.join")
  const router = useRouter()
  const pathname = usePathname()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [blockedCode, setBlockedCode] = useState<string>()
  const [busy, setBusy] = useState(false)
  const approval = party.joinPolicy === "APPROVAL"
  const relation = party.me.relation
  const refresh = () => queryClient.invalidateQueries({ queryKey: partyDetailKey(party.id), exact: true })

  const reasonCode =
    relation === "BANNED"
      ? "B104"
      : party.memberCount >= party.capacity
        ? "B101"
        : party.status === "CLOSED"
          ? "B102"
          : blockedCode

  // 실패는 던진다 (참가 창이 받는다). 막는 코드면 버튼 이유로 남긴다
  const submit = async (input: PartyJoinRequestBody) => {
    try {
      if (approval) {
        await api.v1.party.apply(party.id, input)
        toast.success(t("applied"))
      } else {
        const detail = await api.v1.party.join(party.id, input)
        queryClient.setQueryData(partyDetailKey(party.id), detail)
        toast.success(t("joined"))
      }
      await refresh()
    } catch (error) {
      const code = toApiError(error).code
      if (BLOCKING.has(code)) {
        setBlockedCode(code)
        void refresh()
      }
      throw error
    }
  }

  const start = async () => {
    if (!signedIn) {
      router.push(signInPath(pathname))
      return
    }
    if (needsJoinDialog(party)) {
      setDialogOpen(true)
      return
    }
    setBusy(true)
    try {
      await submit({})
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusy(false)
    }
  }

  const cancelApplication = async () => {
    setBusy(true)
    try {
      await api.v1.party.cancelApplication(party.id)
      toast.success(t("cancelled"))
      await refresh()
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusy(false)
    }
  }

  if (relation === "PENDING") {
    return (
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium">{t("pending")}</span>
        <Button variant="outline" disabled={busy} onClick={cancelApplication}>
          {t("cancelApply")}
        </Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-start gap-1 sm:items-end">
      <Button disabled={busy || !!reasonCode} onClick={start}>
        {approval ? t("apply") : t("join")}
      </Button>
      {reasonCode && (
        <p className="text-sm text-muted-foreground" role="status">
          {t(`reason.${reasonCode as "B101" | "B102" | "B103" | "B104"}`)}
        </p>
      )}
      {reasonCode === "B103" && (
        <LinkButton href={MY_PARTY_PATH} variant="link" size="sm">
          {t("goMyParty")}
        </LinkButton>
      )}
      <JoinDialog
        party={party}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        submitLabel={approval ? t("apply") : t("join")}
        onSubmit={submit}
      />
    </div>
  )
}
