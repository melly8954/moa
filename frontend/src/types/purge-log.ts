// 파기 이력 API의 응답·조건 (moa, specs/013-purge-log. 백엔드 PurgeLogListResponse)

export type PurgeTarget = "LOGIN_HISTORY" | "NOTIFICATION" | "REFRESH_TOKEN" | "RICH_TEXT_IMAGE" | "MEMBER" | "INQUIRY"

export type PurgeLog = {
  id: number
  createdAt: string
  target: PurgeTarget
  // ROW_DELETE(행 삭제), COLUMN_ERASE(개인정보 열 삭제)
  method: "ROW_DELETE" | "COLUMN_ERASE"
  count: number
  // SCHEDULER(정리기), PERSON(사람)
  actor: "SCHEDULER" | "PERSON"
}

// 모두 선택. from·to는 YYYY-MM-DD (양 끝 포함)
export type PurgeLogQuery = {
  target?: string
  from?: string
  to?: string
}
