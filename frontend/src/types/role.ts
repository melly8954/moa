// 역할·권한 API의 응답·요청 (백엔드 rbac 헬퍼: PermissionResponse, RoleResponse, Role*Request)

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
