import type { GrantBlock } from "./role"
import type { Role } from "./user"

// 사용자 관리 API의 응답·요청·조건 (백엔드 user-admin 헬퍼: UserAdminResponse, UserCreateResponse 등)

// moa: 소셜 로그인 제공자 (백엔드 SocialProvider, specs/010-social-login). 디스코드는 백엔드에 더할 때 함께 더한다
export type SocialProvider = "GOOGLE"

export type AdminUser = {
  id: number
  loginId: string
  name: string
  role: Role
  locked: boolean
  lockedAt?: string | null
  // 임시 비밀번호를 받아 아직 바꾸지 않았다
  passwordChangeRequired: boolean
  // false면 비밀번호로 로그인하지 않는 계정(소셜). 비밀번호 초기화를 할 수 없다
  hasPassword: boolean
  createdAt: string
  // moa: 받은 역할 (specs/002-admin-users FR-006). 목록·하나 조회에서만 온다. 회원은 비어 있다
  accessRoles: { id: number; name: string }[]
  // moa: 최고 관리자 역할을 가졌나 (2026-11-21)
  superAdmin: boolean
  // moa: 내가 이 계정을 다룰 수 없는 이유. 다룰 수 있으면 null (research R10)
  protection?: GrantBlock | null
  // moa: 연결된 소셜 제공자 (specs/002-admin-users FR-004, research R17). 목록·하나 조회에서만 온다. 없으면 빈 목록
  socialProviders: SocialProvider[]
}

// 모두 선택. locked는 "true"·"false"
export type AdminUserQuery = { keyword?: string; role?: string; locked?: string }

export type AdminUserCreateRequest = { loginId: string; name: string; role: Role }
// 임시 비밀번호는 이 응답에서만 보인다
export type AdminUserCreateResponse = { user: AdminUser; temporaryPassword: string }
export type TemporaryPassword = { temporaryPassword: string }
