import type { ComponentType } from "react"

// moa: 계정 관리 탭의 항목 (specs/018-member-withdrawal FR-016, research R10)
//   항목은 order 순으로 위에서부터 그린다. "탈퇴하기"는 이 배열 밖에서 늘 맨 아래 오른쪽에 그린다
//   지금은 비어 있다. 비어 있으면 항목 영역을 그리지 않는다 (빈 자리 없음, US4-AC2)
// 항목 더하기 (다른 기능의 스펙이 한다)
//   1. 자기 기능 폴더에 항목 컴포넌트를 만든다. 제목·설명·단추를 그 안에 둔다 (props 없음, 필요한 값은 스스로 읽는다)
//   2. 아래 배열에 { key: "<기능>", order: <숫자>, Component } 한 줄을 더한다. order가 작을수록 위다 (10 단위로 비워 둔다)
//   3. "탈퇴하기"보다 아래에 둘 수 없다. 탈퇴 영역은 이 배열 밖이다
export type AccountManagementItem = {
  key: string
  order: number
  Component: ComponentType
}

export const ACCOUNT_MANAGEMENT_ITEMS: readonly AccountManagementItem[] = []

export function sortedItems(items: readonly AccountManagementItem[]): AccountManagementItem[] {
  return [...items].sort((a, b) => a.order - b.order)
}
