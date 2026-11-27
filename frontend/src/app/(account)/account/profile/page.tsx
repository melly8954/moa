"use client"

import { QueryState } from "@/components/kit/query-state"
import { MypageFrame } from "@/components/profile/mypage-frame"
import { ProfileForm } from "@/components/profile/profile-form"
import { useMyProfile } from "@/components/profile/profile-query"

// moa: 마이페이지 프로필 탭 (specs/009-mypage FR-001·FR-016, US1). 로그인 확인과 셸은 (account) 묶음이 한다 (US1-AC2, research R1)
//   머리·탭·관리자 계정 막기는 MypageFrame이 한다 (specs/018-member-withdrawal research R10).
//   관리자 계정은 권한 없음 안내만 보이고 프로필 API를 부르지 않는다 (US1-AC7)
//   로그인 방식·소셜 연결 영역은 두지 않는다 (US1-AC5)
export default function ProfilePage() {
  return (
    <MypageFrame tab="profile">
      <ProfileTab />
    </MypageFrame>
  )
}

function ProfileTab() {
  const profile = useMyProfile(true)
  return <QueryState query={profile}>{(data) => <ProfileForm profile={data} />}</QueryState>
}
