"use client"

import { useQuery } from "@tanstack/react-query"
import { api } from "@/api"
import { useAuthStore } from "@/stores/auth"
import { STALE_TIME } from "./query"

// 로그인한 사용자 정보와 권한. 로그인한 뒤에만 부른다 (AuthGate 안쪽)
// 로그인·로그아웃할 때 캐시를 비운다 (다른 사용자의 정보가 남지 않게)
export const ME_QUERY_KEY = ["me"] as const
export const PERMISSIONS_QUERY_KEY = ["me", "permissions"] as const

export function useMe() {
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  return useQuery({ queryKey: ME_QUERY_KEY, queryFn: api.v1.user.me, enabled: signedIn })
}

export function useMyPermissions() {
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  return useQuery({
    queryKey: PERMISSIONS_QUERY_KEY,
    queryFn: async () => new Set((await api.v1.user.myPermissions()).permissions),
    enabled: signedIn,
    staleTime: STALE_TIME.REFERENCE,
  })
}

// 내가 이 권한을 가졌나. 버튼·메뉴를 숨길 때만 쓴다 (API는 서버가 막는다)
// 권한을 아직 받지 못했으면 false (보이지 않다가 나타난다)
//   const canManage = useHasPermission("CODE_MANAGE")
export function useHasPermission(key: string): boolean {
  return useMyPermissions().data?.has(key) ?? false
}

// 권한이 있을 때만 children을 그린다
//   <Can permission="CODE_MANAGE"><Button>등록</Button></Can>
export function Can({
  permission,
  fallback = null,
  children,
}: {
  permission: string
  fallback?: React.ReactNode
  children: React.ReactNode
}) {
  return useHasPermission(permission) ? children : fallback
}
