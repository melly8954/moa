"use client"

import { useEffect } from "react"
import { api } from "@/api"
import { useAuthStore } from "@/stores/auth"

// 로그인 없이 여는 화면의 로그인 확인 (사용자 셸, specs/006-notices research R7)
// AuthGate와 달리 로그인을 요구하지 않는다: 처음 열 때 리프레시 토큰으로 한 번 확인하고, 안 되면 로그아웃 상태로 둔다
// 같은 때 AuthGate가 함께 불러도 재발급 요청은 하나로 묶인다 (api/client.ts)
export function SilentAuth({ children }: { children: React.ReactNode }) {
  const status = useAuthStore((s) => s.status)
  useEffect(() => {
    if (status === "unknown") api.v1.auth.refresh().catch(() => useAuthStore.getState().signOut())
  }, [status])
  return children
}
