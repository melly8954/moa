"use client"

import { useQuery } from "@tanstack/react-query"
import { api } from "@/api"
import { ME_QUERY_KEY } from "@/lib/me"

// moa: 내 프로필 조회 (specs/009-mypage). 키를 me 아래에 두어 me를 무효화하면 프로필도 다시 읽는다 (FR-005)
//   로그아웃·로그인할 때 me 캐시와 함께 비워진다
export const PROFILE_QUERY_KEY = [...ME_QUERY_KEY, "profile"] as const

// enabled: 마이페이지를 쓸 수 있는 회원일 때만 부른다 (관리자 계정은 A201이라 부르지 않는다, US1-AC7)
export function useMyProfile(enabled: boolean) {
  return useQuery({ queryKey: PROFILE_QUERY_KEY, queryFn: api.v1.profile.get, enabled })
}
