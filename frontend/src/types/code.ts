// 공통코드 API의 응답·요청 (백엔드 code 헬퍼: CodeListResponse, CodeGroupListResponse, Code*Request)
// moa: 그룹은 관리자가 화면에서 다룬다. 시스템 그룹은 기능이 코드에서 읽어 지울 수 없다 (specs/004-admin-codes FR-004·FR-008)

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
  // moa: 이 그룹을 쓰는 곳
  usedBy?: string | null
  // moa: 시스템 그룹. 마이그레이션으로만 생기고 지울 수 없다
  system: boolean
}

// moa: 공통코드 트리 표의 그룹 하나 (GET /admin/code-groups/tree). 항목은 사용 안 함 포함, 정렬 순서대로
export type CodeGroupTree = CodeGroup & { codes: Code[] }

// moa: 관리자가 만드는 일반 그룹. 그룹 코드는 만든 뒤 바꾸지 않는다
export type CodeGroupCreateRequest = {
  groupCode: string
  name: string
  description?: string
  usedBy?: string
}
export type CodeGroupModifyRequest = Omit<CodeGroupCreateRequest, "groupCode">

export type CodeCreateRequest = {
  // moa: 비우면 그룹 안의 자동 순번 (001, 002, …)
  code?: string
  name: string
  // moa: 비우면 그룹 맨 뒤
  sortOrder?: number
}
export type CodeModifyRequest = {
  name: string
  // moa: 비우면 그대로
  sortOrder?: number
  enabled: boolean
}
export type CodeOrder = { code: string; sortOrder: number }
