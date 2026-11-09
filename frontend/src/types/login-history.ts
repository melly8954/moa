// 접속 이력 API의 응답·조건 (백엔드 login-history 헬퍼: LoginHistoryListResponse)

export type LoginHistory = {
  id: number
  // 실패 이력에는 없다
  userId?: number | null
  // 입력한 아이디
  loginId: string
  success: boolean
  // 실패 코드 (A006, A007 등)
  failureCode?: string | null
  ip?: string | null
  // PC, MOBILE, TABLET, OTHER
  deviceType: string
  os: string
  browser: string
  createdAt: string
}

// 모두 선택. success는 "true"·"false", from·to는 YYYY-MM-DD (양 끝 포함)
export type LoginHistoryQuery = {
  userId?: number
  loginId?: string
  success?: string
  from?: string
  to?: string
}
