"use client"

import { useQuery } from "@tanstack/react-query"
import { Megaphone } from "lucide-react"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { LinkButton } from "@/components/kit/link-button"
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert"
import { useAuthStore } from "@/stores/auth"

export const NEW_NOTICE_QUERY_KEY = ["notices", "new"] as const

// 새 공지 안내 (specs/006-notices US3). 공지 목록을 마지막으로 연 뒤 게시된 공지가 있으면 보인다
// 위치는 임시다 (첫 화면). 사용자 쪽 담당과 맞춘 뒤 옮긴다 (FR-010)
export function NewNoticeAlert() {
  const t = useTranslations("notice")
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const query = useQuery({ queryKey: NEW_NOTICE_QUERY_KEY, queryFn: api.v1.notice.hasNew, enabled: signedIn })
  if (!query.data?.hasNew) return null
  return (
    <Alert>
      <Megaphone aria-hidden />
      <AlertDescription>{t("newNotice")}</AlertDescription>
      <AlertAction>
        <LinkButton href="/notices" size="sm" variant="outline">
          {t("newNoticeLink")}
        </LinkButton>
      </AlertAction>
    </Alert>
  )
}
