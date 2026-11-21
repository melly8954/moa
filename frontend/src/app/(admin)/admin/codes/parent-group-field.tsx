"use client"

import { useTranslations } from "next-intl"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import type { CodeGroupTree } from "@/types/code"

// 새로 만들 때의 "소속 그룹" 칸 (moa, specs/004-admin-codes FR-005, 2026-11-21). 메뉴 관리의 "상위 메뉴" 칸과 같다
//   비우면 새 그룹, 그룹을 고르면 그 그룹의 항목을 만든다. 바꾸면 패널이 그룹·항목 폼으로 바뀐다
export function ParentGroupField({
  value,
  groups,
  onChange,
}: {
  value: string
  groups: CodeGroupTree[]
  onChange: (groupCode: string) => void
}) {
  const t = useTranslations("code")
  return (
    <Field>
      <FieldLabel htmlFor="code-parent-group">{t("parentGroup")}</FieldLabel>
      <NativeSelect
        id="code-parent-group"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-describedby="code-parent-group-hint"
      >
        <NativeSelectOption value="">{t("parentGroupNone")}</NativeSelectOption>
        {groups.map((g) => (
          <NativeSelectOption key={g.groupCode} value={g.groupCode}>
            {g.name}
          </NativeSelectOption>
        ))}
      </NativeSelect>
      <FieldDescription id="code-parent-group-hint">{t("parentGroupHint")}</FieldDescription>
    </Field>
  )
}
