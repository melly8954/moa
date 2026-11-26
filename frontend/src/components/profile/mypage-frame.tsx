"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { QueryState } from "@/components/kit/query-state"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useMe } from "@/lib/me"
import { ACCOUNT_MANAGE_PATH, PROFILE_PATH } from "@/lib/next-path"
import { cn } from "@/lib/utils"

// moa: 마이페이지의 머리·탭·권한 확인 (specs/018-member-withdrawal research R10, specs/009-mypage FR-016)
//   탭은 주소로 나눈 링크다. 저장하지 않은 프로필이 있으면 프로필 폼의 useUnsavedChanges가 링크를 잡아 묻는다
//   관리자 계정(mypageAllowed 거짓)은 권한 없음 안내만 보이고 children을 그리지 않는다 (children 안의 API도 부르지 않는다)
//   지금 탭은 prop으로 받는다
export type MypageTab = "profile" | "manage"

const TABS: { key: MypageTab; href: string }[] = [
  { key: "profile", href: PROFILE_PATH },
  { key: "manage", href: ACCOUNT_MANAGE_PATH },
]

export function MypageFrame({ tab, children }: { tab: MypageTab; children: React.ReactNode }) {
  const t = useTranslations("profile")
  const me = useMe()

  return (
    <Card className="w-full max-w-(--form-max-width)">
      <CardHeader className="gap-4">
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
        <nav aria-label={t("tabs.label")} className="flex gap-4 border-b">
          {TABS.map((item) => (
            <Link
              key={item.key}
              href={item.href}
              aria-current={item.key === tab ? "page" : undefined}
              className={cn(
                "-mb-px border-b-2 px-1 pb-2 text-sm",
                item.key === tab
                  ? "border-primary font-medium text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              {t(`tabs.${item.key}`)}
            </Link>
          ))}
        </nav>
      </CardHeader>
      <CardContent>
        <QueryState query={me}>
          {(m) =>
            m.mypageAllowed ? (
              children
            ) : (
              <Alert variant="destructive" role="alert">
                <AlertDescription>{t("noAccess")}</AlertDescription>
              </Alert>
            )
          }
        </QueryState>
      </CardContent>
    </Card>
  )
}
