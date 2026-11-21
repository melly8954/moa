"use client"

import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"

// 상세 칸의 "사용" 토글 (moa, specs/001-admin-menus FR-010·specs/004-admin-codes FR-005, 2026-11-21)
//   이름 칸 바로 아래 오른쪽에 둔다. 이름 Field 안, 입력 다음에 넣는다
export function EnabledSwitch({
  id,
  label,
  checked,
  onCheckedChange,
  describedBy,
}: {
  id: string
  label: string
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  describedBy?: string
}) {
  return (
    <div className="flex items-center justify-end gap-2">
      <Label htmlFor={id} className="font-normal">
        {label}
      </Label>
      <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} aria-describedby={describedBy} />
    </div>
  )
}
