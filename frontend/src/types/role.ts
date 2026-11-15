// 역할·권한 API의 응답·요청 (백엔드 rbac 헬퍼: PermissionResponse, RoleResponse, Role*Request)
// moa: 권한 트리, 사용자별 역할, 구성원 (PermissionTreeResponse, RoleUserResponse. specs/003-admin-roles FR-004~FR-006)

// 코드에 등록된 권한. group은 화면에서 묶을 이름
export type Permission = { key: string; name: string; group: string }

export type Role = {
  id: number
  // 영문 대문자·숫자·밑줄. 만든 뒤 바꾸지 않는다
  code: string
  name: string
  description?: string | null
  // 묶은 권한 키 (이름순)
  permissions: string[]
  // 이 역할을 가진 사용자 수
  userCount: number
}

export type RoleCreateRequest = { code: string; name: string; description?: string; permissions: string[] }
export type RoleModifyRequest = { name: string; description?: string; permissions: string[] }

export type PermissionRef = { key: string; name: string }

// 역할 창의 관리 메뉴 트리. 묶음 메뉴는 permission이 없고 children이 있다. 꺼진 메뉴도 들어 있다
export type PermissionTreeNode = {
  menuId: number
  name: string
  enabled: boolean
  permission?: PermissionRef | null
  children: PermissionTreeNode[]
}
export type PermissionTree = {
  menus: PermissionTreeNode[]
  // 어느 관리 메뉴도 가리키지 않는 관리 프로그램
  unlisted: { programKey: string; name: string; permission: PermissionRef }[]
}

// 역할 관리에서 보는 사용자. role은 시스템 역할 (ADMIN은 업무 역할을 받지 않는다)
export type RoleUser = {
  id: number
  loginId: string
  name: string
  role: "USER" | "ADMIN"
  accessRoles: { id: number; code: string; name: string }[]
}
export type RoleUserQuery = { keyword?: string }
