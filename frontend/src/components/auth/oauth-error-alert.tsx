"use client"

import { useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"
import { Alert, AlertDescription } from "@/components/ui/alert"

// moa: 소셜 로그인 실패 안내 (specs/010-social-login US3, contracts 콜백 표)
// 콜백이 /sign-in?oauthError=<코드>로 보낸다. 쿼리가 없거나 모르는 값이면 아무것도 보이지 않는다 (취소는 쿼리 없음, US3-AC1)
// useSearchParams를 쓰므로 부르는 쪽이 <Suspense>로 감싼다 (Next 16 정적 렌더링)
const CODES = ["failed", "locked", "disabled", "emailUnverified", "emailInUse"] as const
type OAuthErrorCode = (typeof CODES)[number]

function isOAuthErrorCode(value: string | null): value is OAuthErrorCode {
  return value !== null && (CODES as readonly string[]).includes(value)
}

export function OAuthErrorAlert() {
  const code = useSearchParams().get("oauthError")
  const t = useTranslations("signIn.oauthError")
  if (!isOAuthErrorCode(code)) return null
  return (
    <Alert variant="destructive">
      <AlertDescription>{t(code)}</AlertDescription>
    </Alert>
  )
}
