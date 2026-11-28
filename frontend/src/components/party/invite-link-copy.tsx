"use client"

import { Copy } from "lucide-react"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import type { PartyInvite } from "@/types/party"

// 초대 링크를 화면 주소로 바꾼다. 서버가 경로(/invite/{code})만 주면 지금 사이트 주소를 앞에 붙인다
export function inviteUrlOf(invite: PartyInvite): string {
  if (/^https?:\/\//.test(invite.url)) return invite.url
  const path = invite.url.startsWith("/") ? invite.url : `/invite/${invite.code}`
  return typeof window === "undefined" ? path : `${window.location.origin}${path}`
}

// 초대 코드·링크 복사 (specs/008-party US4). 만들기 창(비공개)과 파티 화면이 함께 쓴다
export function InviteLinkCopy({ invite }: { invite: PartyInvite }) {
  const t = useTranslations("party.invite")
  const url = inviteUrlOf(invite)
  const copy = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value)
      toast.success(t("copied"))
    } catch {
      toast.error(t("copyFailed"))
    }
  }
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <Input readOnly value={url} aria-label={t("link")} className="min-w-0" />
        <Button variant="outline" onClick={() => void copy(url)}>
          <Copy />
          {t("copyLink")}
        </Button>
      </div>
      <div className="flex items-center gap-2">
        <Input readOnly value={invite.code} aria-label={t("code")} className="min-w-0 font-mono" />
        <Button variant="outline" onClick={() => void copy(invite.code)}>
          <Copy />
          {t("copyCode")}
        </Button>
      </div>
    </div>
  )
}
