"use client"

import { useTranslations } from "next-intl"
import { Badge } from "@/components/ui/badge"
import type { AdminUser } from "@/types/user-admin"

// moa: 소셜 계정 표시 (specs/002-admin-users FR-004·FR-007, research R17)

type Translate = ReturnType<typeof useTranslations<"userAdmin">>

// 목록 아이디 뒤에 붙는 연결 제공자 배지. 연결이 없으면 아무것도 그리지 않는다
export function SocialBadges({ user }: { user: AdminUser }) {
  const t = useTranslations("userAdmin")
  return user.socialProviders.map((provider) => (
    <Badge key={provider} variant="outline" className="font-normal">
      {t(`providers.${provider}`)}
    </Badge>
  ))
}

// 드로어의 로그인 방식: "아이디·비밀번호", "구글", "아이디·비밀번호, 구글"
export function loginMethodText(user: AdminUser, t: Translate) {
  const methods = user.socialProviders.map((provider) => t(`providers.${provider}`))
  if (user.hasPassword) methods.unshift(t("loginMethodPassword"))
  return methods.join(", ")
}

// 비밀번호 줄 설명. 비밀번호가 없으면 꺼진 이유와 할 일을 보인다 (FR-007)
export function passwordHint(user: AdminUser, t: Translate) {
  if (user.hasPassword) return t("resetHint")
  if (user.socialProviders.length === 0) return t("noPasswordHint")
  return t("socialOnlyHint", {
    providers: user.socialProviders.map((provider) => t(`providers.${provider}`)).join("·"),
  })
}
