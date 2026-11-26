import type { ComponentType } from "react"

// moa: 계정 관리 탭의 항목 (specs/018-member-withdrawal FR-016, research R10)
//   항목은 order 순으로 위에서부터 그린다. "탈퇴하기"는 이 배열 밖에서 늘 맨 아래 오른쪽에 그린다
//   지금은 비어 있다. 비어 있으면 항목 영역을 그리지 않는다 (빈 자리 없음, US4-AC2)
export type AccountManagementItem = {
  key: string
  order: number
  Component: ComponentType
}

export const ACCOUNT_MANAGEMENT_ITEMS: readonly AccountManagementItem[] = []

export function sortedItems(items: readonly AccountManagementItem[]): AccountManagementItem[] {
  return [...items].sort((a, b) => a.order - b.order)
}
