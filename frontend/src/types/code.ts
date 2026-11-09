// 공통코드 API의 응답·요청 (백엔드 code 헬퍼: CodeListResponse, CodeGroupListResponse, Code*Request)

export type Code = {
  code: string
  name: string
  sortOrder: number
  // false면 사용 안 함 (화면 목록에는 includeDisabled일 때만 온다)
  enabled: boolean
}

export type CodeGroup = {
  groupCode: string
  name: string
  description?: string | null
}

export type CodeGroupCreateRequest = {
  groupCode: string
  name: string
  description?: string
}
export type CodeCreateRequest = {
  code: string
  name: string
  sortOrder: number
}
export type CodeModifyRequest = {
  name: string
  sortOrder: number
  enabled: boolean
}
