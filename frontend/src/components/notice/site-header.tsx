"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { useEffect } from "react"
import { api } from "@/api"
import { LinkButton } from "@/components/kit/link-button"
import { SITE } from "@/lib/site"
import { signInPath } from "@/lib/next-path"
import { useAuthStore } from "@/stores/auth"

// 로그인 없이 보는 화면의 머리 ((site) 레이아웃)
// AuthGate와 달리 로그인을 요구하지 않는다: 처음 열 때 리프레시 토큰으로 한 번 확인하고, 안 되면 그대로 둔다
export function SiteHeader() {
  const t = useTranslations("notice")
  const status = useAuthStore((s) => s.status)

  useEffect(() => {
    if (status === "unknown") api.v1.auth.refresh().catch(() => useAuthStore.getState().signOut())
  }, [status])

  return (
    <header className="flex h-(--header-height) shrink-0 items-center justify-between gap-2 border-b px-(--page-padding)">
      <Link href="/notices" className="font-medium">
        {SITE.name}
      </Link>
      {status === "signedIn" ? (
        <LinkButton href="/" variant="outline" size="sm">
          {t("home")}
        </LinkButton>
      ) : status === "unknown" ? null : (
        <LinkButton href={signInPath("/notices")} variant="outline" size="sm">
          {t("signIn")}
        </LinkButton>
      )}
    </header>
  )
}
