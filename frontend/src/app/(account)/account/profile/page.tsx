"use client"

import { useTranslations } from "next-intl"
import { QueryState } from "@/components/kit/query-state"
import { ProfileForm } from "@/components/profile/profile-form"
import { useMyProfile } from "@/components/profile/profile-query"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useMe } from "@/lib/me"

// moa: 마이페이지 (specs/009-mypage FR-001·FR-016, US1). 로그인 확인과 셸은 (account) 묶음이 한다 (US1-AC2, research R1)
//   관리자 계정(mypageAllowed 거짓)은 권한 없음 안내만 보이고 프로필 API를 부르지 않는다 (US1-AC7)
//   로그인 방식·소셜 연결 영역은 두지 않는다 (US1-AC5)
export default function ProfilePage() {
  const t = useTranslations("profile")
  const me = useMe()
  const allowed = me.data?.mypageAllowed === true
  const profile = useMyProfile(allowed)

  return (
    <Card className="w-full max-w-(--form-max-width)">
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <QueryState query={me}>
          {(m) =>
            m.mypageAllowed ? (
              <QueryState query={profile}>{(data) => <ProfileForm profile={data} />}</QueryState>
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
