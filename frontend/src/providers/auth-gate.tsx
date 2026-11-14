"use client"

import { useQueryClient } from "@tanstack/react-query"
import { usePathname, useRouter } from "next/navigation"
import { useEffect } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { QueryState } from "@/components/kit/query-state"
import { Skeleton } from "@/components/ui/skeleton"
import { useErrorMessages } from "@/lib/error-messages"
import { useMe } from "@/lib/me"
import { PASSWORD_PATH, signInPath } from "@/lib/next-path"
import { useAuthStore } from "@/stores/auth"

// 로그인한 사용자만 보는 화면을 감싼다 ((admin)·(account) 레이아웃, (user)/(signed-in) 레이아웃)
//   처음 열거나 새로고침하면 리프레시 토큰 쿠키로 액세스 토큰을 다시 받고, 내 정보(/users/me)를 읽는다
//   받지 못하면 로그인 화면으로 보낸다. 지금 주소를 ?next=로 붙여 로그인 뒤 돌아온다 (로그아웃을 누른 때는 붙이지 않는다)
//   쓰는 중에 재발급이 실패하면 안내를 띄우고 보낸다
//   임시 비밀번호를 받은 사용자(passwordChangeRequired)는 비밀번호 변경 화면에만 둔다
// 화면을 가릴 뿐이다. API 권한은 백엔드가 막는다
export function AuthGate({ children }: { children: React.ReactNode }) {
  const status = useAuthStore((s) => s.status)
  const router = useRouter()
  const pathname = usePathname()
  const queryClient = useQueryClient()
  const { errorMessage } = useErrorMessages()
  const me = useMe()
  const mustChangePassword = me.data?.passwordChangeRequired === true && pathname !== PASSWORD_PATH

  useEffect(() => {
    if (status === "unknown") {
      api.v1.auth.refresh().catch(() => useAuthStore.getState().signOut())
    } else if (status === "expired") {
      toast.info(errorMessage("A003"))
      useAuthStore.getState().signOut()
    } else if (status === "signedOut") {
      queryClient.clear()
      const byUser = useAuthStore.getState().signedOutByUser
      router.replace(byUser ? signInPath() : signInPath(window.location.pathname + window.location.search))
    }
  }, [status, router, queryClient, errorMessage])

  useEffect(() => {
    if (mustChangePassword) router.replace(PASSWORD_PATH)
  }, [mustChangePassword, router])

  const waiting = (
    <div className="flex min-h-svh flex-col gap-3 p-(--page-padding)" aria-busy="true">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
    </div>
  )
  if (status !== "signedIn" || mustChangePassword) return waiting
  return (
    <QueryState query={me} loading={waiting} wrapError={(alert) => <div className="p-(--page-padding)">{alert}</div>}>
      {() => children}
    </QueryState>
  )
}
