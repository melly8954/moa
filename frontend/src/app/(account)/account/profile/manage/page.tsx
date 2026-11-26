"use client"

import { ACCOUNT_MANAGEMENT_ITEMS, sortedItems } from "@/components/account-management/items"
import { WithdrawSection } from "@/components/account-management/withdraw-section"
import { MypageFrame } from "@/components/profile/mypage-frame"
import { useMe } from "@/lib/me"

// moa: 마이페이지 계정 관리 탭 (specs/018-member-withdrawal contracts/screens.md, US1-AC1, US4-AC1·AC2)
//   더해진 항목을 순서대로 그리고, 맨 아래 오른쪽에 "탈퇴하기". 항목이 없으면 탈퇴 영역만
//   관리자 계정은 MypageFrame이 권한 없음 안내만 보인다
export default function AccountManagePage() {
  return (
    <MypageFrame tab="manage">
      <AccountManageTab />
    </MypageFrame>
  )
}

function AccountManageTab() {
  const me = useMe()
  const items = sortedItems(ACCOUNT_MANAGEMENT_ITEMS)

  return (
    <div className="flex flex-col gap-6">
      {items.length > 0 && (
        <div className="flex flex-col gap-6">
          {items.map(({ key, Component }) => (
            <Component key={key} />
          ))}
        </div>
      )}
      <WithdrawSection nickname={me.data?.name ?? ""} />
    </div>
  )
}
