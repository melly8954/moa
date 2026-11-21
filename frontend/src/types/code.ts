// 공통코드 API의 응답·요청 (백엔드 code 헬퍼: CodeListResponse, CodeGroupListResponse, Code*Request)
// moa: 그룹은 관리자가 화면에서 다룬다. 그룹과 항목은 지우지 않고 사용 여부로 끈다 (specs/004-admin-codes FR-004·FR-008)

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
  // moa: 사용 여부. false면 화면용 목록이 빈 목록이다 (지우지 않고 끈다)
  enabled: boolean
}

// moa: 공통코드 트리 표의 그룹 하나 (GET /admin/code-groups/tree). 항목은 사용 안 함 포함, 정렬 순서대로
export type CodeGroupTree = CodeGroup & { codes: Code[] }

// moa: 관리자가 만드는 그룹 (사용 중으로 생긴다). 그룹 코드는 만든 뒤 바꾸지 않는다
export type CodeGroupCreateRequest = {
  groupCode: string
  name: string
  description?: string
}
export type CodeGroupModifyRequest = { name: string; description?: string; enabled: boolean }

export type CodeCreateRequest = {
  // moa: 필수. 자동 순번은 없다 (specs/004-admin-codes FR-006, 2026-11-21)
  code: string
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
// moa: 그룹 순서 한 줄 (PUT /admin/code-groups/order, 2026-11-21)
export type CodeGroupOrder = { groupCode: string; sortOrder: number }
// moa: 한 그룹 안 항목의 사용 여부 한꺼번에 (PUT /admin/code-groups/{g}/codes/enabled)
export type CodeEnabledRequest = { codes: string[]; enabled: boolean }
