"use client"

import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useEffect } from "react"
import { api } from "@/api"
import { fetchLandingPath } from "@/components/shell/landing"
import { Spinner } from "@/components/ui/spinner"
import { currentNextPath } from "@/lib/next-path"

// moa: 소셜 로그인 마무리 (specs/010-social-login research R4)
// 콜백이 리프레시 쿠키만 심고 여기로 보낸다. 액세스 토큰은 주소에 싣지 않고 기존 재발급으로 받는다
// 성공하면 기존 로그인과 같은 첫 화면 규칙(next 또는 landingPath), 실패하면 로그인 화면에 실패 안내
export default function OAuthDonePage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const t = useTranslations("oauthDone")

  useEffect(() => {
    let cancelled = false
    const finish = async () => {
      try {
        queryClient.clear()
        // 재발급이 useAuthStore.signIn을 부른다. 동시에 두 번 불려도 요청은 하나다 (api/client.ts)
        await api.v1.auth.refresh()
        const next = currentNextPath() ?? (await fetchLandingPath(queryClient))
        if (!cancelled) router.replace(next)
      } catch {
        if (!cancelled) router.replace("/sign-in?oauthError=failed")
      }
    }
    void finish()
    return () => {
      cancelled = true
    }
  }, [queryClient, router])

  return (
    <div role="status" className="flex items-center gap-2 text-muted-foreground">
      <Spinner />
      <span>{t("loading")}</span>
    </div>
  )
}
