"use client"

import { useQuery } from "@tanstack/react-query"
import { api } from "@/api"
import { STALE_TIME } from "./query"

// 공통코드 목록. 그룹별로 5분 동안 다시 묻지 않는다 (기준 데이터)
//   const statuses = useCodes("ORDER_STATUS")
// includeDisabled: 사용 안 함 코드도 받는다. 예전 값의 이름을 보일 때(CodeName) 쓴다
export const codesQueryKey = (groupCode: string, includeDisabled = false) =>
  ["codes", groupCode, includeDisabled] as const

export function useCodes(groupCode: string, { includeDisabled = false }: { includeDisabled?: boolean } = {}) {
  return useQuery({
    queryKey: codesQueryKey(groupCode, includeDisabled),
    queryFn: () => api.v1.code.list(groupCode, { includeDisabled }),
    staleTime: STALE_TIME.REFERENCE,
  })
}
