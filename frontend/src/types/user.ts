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
}

export type MyPermissions = { permissions: string[] }

export type PasswordChangeRequest = { currentPassword: string; newPassword: string }
