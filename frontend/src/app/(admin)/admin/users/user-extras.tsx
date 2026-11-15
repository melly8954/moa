"use client"

import type { AdminUser } from "@/types/user-admin"
// 다른 헬퍼가 사용자 관리 화면에 더하는 것
// moa: rbac 헬퍼의 "업무 역할"은 뺐다. 업무 역할은 역할 관리에서만 준다 (specs/002-admin-users FR-005)
// apply가 아래 표시 위에 줄을 끼운다. 표시 줄은 지우지 않는다
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
    // @helpers:user-actions
  ]
}

// 화면에 함께 둘 창
const DIALOGS: React.ComponentType[] = [
  // @helpers:user-dialogs
]

export function UserExtraDialogs() {
  return DIALOGS.map((Dialog, i) => <Dialog key={i} />)
}
