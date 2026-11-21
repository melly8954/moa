"use client"

import { useRouter } from "next/navigation"
import { useEffect } from "react"
import { useMe } from "@/lib/me"
import { useAuthStore } from "@/stores/auth"
import { ADMIN_HOME } from "./landing"

// 사용자 셸 화면을 관리자 계정이 열지 못하게 한다 (moa, specs/001-admin-menus FR-005, 2026-11-22)
//   관리자 계정(ADMIN)이면 관리 셸 첫 화면(/admin → 볼 수 있는 첫 관리 메뉴)으로 보내고 방문 기록을 남기지 않는다
//   로그인하지 않았거나 회원이면 그대로 보인다. 로그인한 사람의 정보를 읽는 동안은 화면을 그리지 않아 사용자 화면이 잠깐 보이지 않게 한다
//   화면을 가릴 뿐이다. 관리자 계정이 회원 활동을 못 하게 막는 것은 각 기능의 API가 한다
export function MemberOnly({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const me = useMe()
  const admin = me.data?.role === "ADMIN"
  useEffect(() => {
    if (admin) router.replace(ADMIN_HOME)
  }, [admin, router])
  if (admin || (signedIn && me.isPending)) return null
  return children
}
