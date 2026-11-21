// 역할·권한 API의 응답·요청 (백엔드 rbac 헬퍼: PermissionResponse, RoleResponse, Role*Request)
// moa: 권한 트리, 구성원 (PermissionTreeResponse, RoleMemberResponse. specs/003-admin-roles FR-004~FR-006)

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
  // moa: 코드에 고정된 최고 관리자 역할. 권한 키가 비어 있고 모든 권한을 갖는다. 바꾸거나 지울 수 없다 (specs/003-admin-roles FR-010)
  fixed: boolean
  // moa: 묶은 권한의 이름 (사용자 관리 드로어에 작게 보인다). 최고 관리자 역할은 빈다
  permissionNames: string[]
  // moa: 내가 이 역할을 다루고 주고 뺄 수 있나. 못 하면 blockReason이 이유다 (research R18)
  grantable: boolean
  blockReason?: GrantBlock | null
}

// moa: 다룰 수 없는 이유. FIXED는 최고 관리자 역할(누구도 주거나 빼지 못함, 한 명 고정), SUPER_ADMIN_ONLY는 최고 관리자만,
//   WIDER_PERMISSIONS는 내가 갖지 않은 권한이 들어 있음 (specs/003-admin-roles FR-010, research R23)
export type GrantBlock = "FIXED" | "SUPER_ADMIN_ONLY" | "WIDER_PERMISSIONS"

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
  // 어느 관리 메뉴도 가리키지 않는 관리 연결 화면
  unlisted: { screenKey: string; name: string; permission: PermissionRef }[]
}

// 역할 구성원의 한 줄 (moa, specs/003-admin-roles FR-006). 관리자 계정만 온다
export type RoleMember = {
  id: number
  loginId: string
  name: string
  // 최고 관리자 역할을 가졌나. 다른 역할은 받지 않는다
  superAdmin: boolean
  // 이 역할의 구성원인가
  member: boolean
}
export type RoleMemberQuery = { keyword?: string }
