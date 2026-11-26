"use client"

import { useTranslations } from "next-intl"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { WithdrawDialog } from "./withdraw-dialog"

// moa: 계정 관리 탭 맨 아래 오른쪽의 "탈퇴하기" (specs/018-member-withdrawal US1-AC1). 누르면 확인 창을 연다
export function WithdrawSection({ nickname }: { nickname: string }) {
  const t = useTranslations("accountManagement.withdraw")
  const [open, setOpen] = useState(false)

  return (
    <div className="flex justify-end">
      <Button variant="destructive" onClick={() => setOpen(true)}>
        {t("open")}
      </Button>
      <WithdrawDialog open={open} onOpenChange={setOpen} nickname={nickname} />
    </div>
  )
}
