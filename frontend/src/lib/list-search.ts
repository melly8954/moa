"use client"

import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { useCallback, useMemo, useState } from "react"

// 목록 화면의 검색 조건·쪽·정렬. 모두 주소(쿼리 문자열)에 둔다
//   → 새로고침·뒤로 가기·주소 공유를 해도 같은 목록이 열린다
// 규칙 (목록 화면 공통 모양)
//   - 조건을 바꿔도 바로 반영하지 않는다. 조회를 눌러야 주소가 바뀌고, 1쪽으로 돌아간다
//   - 초기화는 기본값으로 되돌리고 바로 조회한다
//   - 쪽 크기·정렬을 바꾸면 1쪽으로 돌아간다
//   - 주소의 쪽은 1부터, API의 page는 0부터다 (CONTRACT.md PageResponse)
// 조건 값은 모두 문자열이다. 빈 문자열은 조건 없음이고 주소·API에 넣지 않는다

export const PAGE_SIZES = [10, 20, 50] as const
export type PageSize = (typeof PAGE_SIZES)[number]

export type SortDirection = "asc" | "desc"
export type Sort = { field: string; direction: SortDirection } | undefined

type Filters = Record<string, string>

export type ListSearch<F extends Filters> = {
  // 지금 목록에 반영된 조건 (주소에서 읽음)
  filters: F
  // 검색 영역에 입력 중인 조건 (조회를 눌러야 반영)
  draft: F
  setDraft: (patch: Partial<F>) => void
  // 1부터
  page: number
  size: PageSize
  sort: Sort
  // 기본값과 다른 조건이 반영돼 있나 (빈 결과 문구를 "검색 결과 없음"으로 고른다)
  isFiltered: boolean
  submit: (patch?: Partial<F>) => void
  reset: () => void
  setPage: (page: number) => void
  setSize: (size: PageSize) => void
  setSort: (sort: Sort) => void
  // API에 넘길 값: 빈 조건은 빼고, page(0부터)·size·sort("필드,asc")를 붙인다. queryKey에도 쓴다
  params: Partial<F> & { page: number; size: number; sort?: string }
}

function readSort(value: string | null): Sort {
  const [field, direction] = (value ?? "").split(",")
  return field && (direction === "asc" || direction === "desc") ? { field, direction } : undefined
}

function readSize(value: string | null, fallback: PageSize): PageSize {
  const size = Number(value)
  return PAGE_SIZES.includes(size as PageSize) ? (size as PageSize) : fallback
}

export function useListSearch<F extends Filters>({
  defaults,
  size: defaultSize = 20,
  sort: defaultSort,
}: {
  defaults: F
  size?: PageSize
  sort?: Sort
}): ListSearch<F> {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const query = searchParams.toString()

  const state = useMemo(() => {
    const params = new URLSearchParams(query)
    const filters = { ...defaults }
    for (const key of Object.keys(defaults) as (keyof F)[]) {
      const value = params.get(key as string)
      if (value !== null) filters[key] = value as F[keyof F]
    }
    const page = Math.max(1, Math.floor(Number(params.get("page")) || 1))
    const size = readSize(params.get("size"), defaultSize)
    const sort = params.has("sort") ? readSort(params.get("sort")) : defaultSort
    return { filters, page, size, sort }
    // defaults·defaultSort는 화면마다 고정값이라 주소가 바뀔 때만 다시 읽는다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query])

  // 입력 중인 조건. 주소가 바뀌면(조회, 뒤로 가기) 반영된 조건으로 맞춘다
  const [draft, setDraftState] = useState(state.filters)
  const [lastQuery, setLastQuery] = useState(query)
  if (query !== lastQuery) {
    setLastQuery(query)
    setDraftState(state.filters)
  }

  const navigate = useCallback(
    (next: { filters: F; page: number; size: PageSize; sort: Sort }) => {
      const params = new URLSearchParams()
      for (const [key, value] of Object.entries(next.filters)) if (value !== "") params.set(key, value)
      if (next.page > 1) params.set("page", String(next.page))
      if (next.size !== defaultSize) params.set("size", String(next.size))
      const sortText = next.sort ? `${next.sort.field},${next.sort.direction}` : ""
      const defaultSortText = defaultSort ? `${defaultSort.field},${defaultSort.direction}` : ""
      if (sortText !== defaultSortText) params.set("sort", sortText)
      const text = params.toString()
      router.push(text ? `${pathname}?${text}` : pathname, { scroll: false })
    },
    [router, pathname, defaultSize, defaultSort],
  )

  const params = useMemo(() => {
    const result: Record<string, string | number> = {}
    for (const [key, value] of Object.entries(state.filters)) if (value !== "") result[key] = value
    result.page = state.page - 1
    result.size = state.size
    if (state.sort) result.sort = `${state.sort.field},${state.sort.direction}`
    return result as ListSearch<F>["params"]
  }, [state])

  return {
    ...state,
    draft,
    setDraft: (patch) => setDraftState((prev) => ({ ...prev, ...patch })),
    isFiltered: Object.keys(defaults).some((key) => state.filters[key] !== defaults[key]),
    submit: (patch = {}) => navigate({ ...state, filters: { ...draft, ...patch }, page: 1 }),
    reset: () => {
      setDraftState(defaults)
      navigate({ filters: defaults, page: 1, size: state.size, sort: defaultSort })
    },
    setPage: (page) => navigate({ ...state, page }),
    setSize: (size) => navigate({ ...state, size, page: 1 }),
    setSort: (sort) => navigate({ ...state, sort, page: 1 }),
    params,
  }
}
