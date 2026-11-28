"use client"

import { useQuery } from "@tanstack/react-query"
import { api } from "@/api"

export const SUPPORT_CATEGORIES_KEY = ["support", "categories"] as const

// 고객센터 분류 고르기용 선택지 (specs/017-customer-center research R8)
//   지금 고를 수 있는 항목만, 코드 관리의 순서대로. 로그인 없이 부른다 (FAQ 화면)
//   저장된 분류의 이름은 이것으로 찾지 않고 응답의 categoryName을 쓴다 (꺼진 분류도 이름이 남는다)
export function useSupportCategories() {
  return useQuery({ queryKey: SUPPORT_CATEGORIES_KEY, queryFn: api.v1.faq.categories, staleTime: 60_000 })
}
