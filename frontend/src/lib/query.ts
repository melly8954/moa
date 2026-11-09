// 서버 데이터를 얼마 동안 새것으로 볼지 (TanStack Query staleTime)
//   DEFAULT: 전역 기본. AppProviders가 QueryClient에 넣는다
//   REFERENCE: 자주 바뀌지 않는 기준 데이터 (공통코드, 메뉴, 내 권한). useQuery에 staleTime으로 준다
export const STALE_TIME = {
  DEFAULT: 30_000,
  REFERENCE: 5 * 60_000,
} as const
