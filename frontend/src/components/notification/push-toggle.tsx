"use client"

import { useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useEffect, useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { Card, CardContent } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { useErrorMessages } from "@/lib/error-messages"
import { disablePush, enablePush, type PushState, pushState } from "@/lib/web-push"

// 브라우저 알림 켜기·끄기 (specs/007-notifications US4, contracts/notifications-screens.md)
//   지원 안 함 / 권한 거부(브라우저 설정 안내) / 서버 웹 푸시 꺼짐(공개 키 없음) 안내
export function PushToggle() {
  const t = useTranslations("notification")
  const { describeError } = useErrorMessages()
  const [state, setState] = useState<PushState | undefined>()
  const [busy, setBusy] = useState(false)
  const publicKey = useQuery({ queryKey: ["notifications", "push-key"], queryFn: api.v1.notification.pushPublicKey })

  useEffect(() => {
    void pushState().then(setState)
  }, [])

  const toggle = async (on: boolean) => {
    setBusy(true)
    try {
      if (on && publicKey.data?.publicKey) {
        const next = await enablePush(publicKey.data.publicKey)
        setState(next)
        if (next === "on") toast.success(t("pushOn"))
      } else {
        await disablePush()
        setState("off")
        toast.success(t("pushOff"))
      }
    } catch (error) {
      toast.error(describeError(error).message)
    } finally {
      setBusy(false)
    }
  }

  const serverOff = publicKey.isSuccess && !publicKey.data.publicKey
  const note =
    state === "unsupported"
      ? t("pushUnsupported")
      : state === "denied"
        ? t("pushDenied")
        : serverOff
          ? t("pushServerOff")
          : t("pushDescription")

  return (
    <Card>
      <CardContent className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <label htmlFor="push-toggle" className="text-sm font-medium">
            {t("push")}
          </label>
          <p id="push-note" className="text-sm text-muted-foreground">
            {note}
          </p>
        </div>
        <Switch
          id="push-toggle"
          aria-label={t("push")}
          aria-describedby="push-note"
          checked={state === "on"}
          disabled={busy || !state || state === "unsupported" || state === "denied" || serverOff}
          onCheckedChange={(checked) => void toggle(checked)}
        />
      </CardContent>
    </Card>
  )
}
