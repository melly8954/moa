"use client"

import { useQuery } from "@tanstack/react-query"
import { api } from "@/api"
import { useAuthStore } from "@/stores/auth"

export const NEW_NOTICE_QUERY_KEY = ["notices", "new"] as const

// 새 공지가 있나 (specs/006-notices US3). 공지 목록을 마지막으로 연 뒤 게시된 공지가 있으면 true
// 셸 "공지" 메뉴 옆 점이 쓴다 (use-menu.ts, FR-010). 로그인하지 않았으면 묻지 않는다
export function useHasNewNotice(): boolean {
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const query = useQuery({ queryKey: NEW_NOTICE_QUERY_KEY, queryFn: api.v1.notice.hasNew, enabled: signedIn })
  return query.data?.hasNew === true
}
