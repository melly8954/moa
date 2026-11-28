// 골격 사용자 API의 응답·요청 (백엔드 UserGetResponse, MyPermissionResponse, PasswordChangeRequest)
// 키트는 백엔드 없이 만들어지므로 손으로 적는다. 프로젝트에서 gen:api를 돌린 뒤 Schema<"UserGetResponse">로 바꿔도 된다

export type Role = "USER" | "ADMIN"

export type Me = {
  id: number
  loginId: string
  name: string
  role: Role
  // 임시 비밀번호를 받아 바꿔야 한다. true면 AuthGate가 비밀번호 변경 화면으로 보낸다
  passwordChangeRequired: boolean
  // moa: false면 비밀번호 없는 계정(소셜). 계정 메뉴에 비밀번호 변경을 보이지 않는다 (specs/010-social-login US2-AC3)
  hasPassword: boolean
  // moa: 계정 바 아바타 키. 프로필이 없거나 목록에서 빠진 키면 default (specs/009-mypage FR-014)
  avatar: string
  // moa: false면 관리자 계정(ADMIN). 마이페이지 메뉴를 숨기고 /account/profile에서 권한 없음 (specs/009-mypage FR-016)
  mypageAllowed: boolean
}

export type MyPermissions = { permissions: string[] }

export type PasswordChangeRequest = { currentPassword: string; newPassword: string }
