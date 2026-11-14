"use client"

import type { AdminUser } from "@/types/user-admin"
// 다른 헬퍼가 사용자 관리 화면에 더하는 것 (예: rbac 헬퍼의 "업무 역할")
// apply가 아래 표시 위에 줄을 끼운다. 표시 줄은 지우지 않는다
import { UserRolesDialog, useUserRolesAction } from "@/components/role/user-roles"
// @helpers:imports

// 행의 "관리" 메뉴에 더할 항목. 항목마다 hook 하나를 부른다
export type UserAction = {
  key: string
  label: string
  disabled?: (user: AdminUser) => boolean
  onSelect: (user: AdminUser) => void
}

export function useUserExtraActions(): UserAction[] {
  return [
    useUserRolesAction(),
    // @helpers:user-actions
  ]
}

// 화면에 함께 둘 창
const DIALOGS: React.ComponentType[] = [
  UserRolesDialog,
  // @helpers:user-dialogs
]

export function UserExtraDialogs() {
  return DIALOGS.map((Dialog, i) => <Dialog key={i} />)
}
