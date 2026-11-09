"use client"

import { useTranslations } from "next-intl"
import { ChoiceFilter, SelectFilter } from "@/components/kit/search-panel"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { useCodes } from "@/lib/codes"

// 공통코드 선택 상자 (폼). 사용하는 코드만 고를 수 있다
//   <CodeSelect groupCode="ORDER_STATUS" value={field.value} onChange={field.onChange} id="status" />
// 지금 값이 사용 안 함 코드면 그 코드도 목록에 남겨 둔다 (모르는 사이 값이 바뀌지 않게)
export function CodeSelect({
  groupCode,
  value,
  onChange,
  placeholder,
  ...props
}: {
  groupCode: string
  value: string
  onChange: (value: string) => void
  // 맨 앞 빈 선택지 글자. 기본은 "선택"
  placeholder?: string
} & Omit<React.ComponentProps<typeof NativeSelect>, "value" | "onChange">) {
  const t = useTranslations("code")
  const all = useCodes(groupCode, { includeDisabled: true }).data ?? []
  const options = all.filter((c) => c.enabled || c.code === value)
  return (
    <NativeSelect {...props} value={value} onChange={(e) => onChange(e.target.value)}>
      <NativeSelectOption value="">{placeholder ?? t("choose")}</NativeSelectOption>
      {options.map((c) => (
        <NativeSelectOption key={c.code} value={c.code}>
          {c.enabled ? c.name : t("disabledName", { name: c.name })}
        </NativeSelectOption>
      ))}
    </NativeSelect>
  )
}

// 검색 영역 조건 (SearchPanel 안 SearchField에 둔다). 코드가 4개 이하면 누름 단추, 5개 이상이면 선택 상자
//   <SearchField label={t("status")}><CodeFilter name="status" groupCode="ORDER_STATUS" /></SearchField>
export function CodeFilter({ name, groupCode }: { name: string; groupCode: string }) {
  const codes = useCodes(groupCode).data ?? []
  const options = codes.map((c) => ({ value: c.code, label: c.name }))
  return options.length <= 4 ? (
    <ChoiceFilter name={name} options={options} />
  ) : (
    <SelectFilter name={name} options={options} />
  )
}

// 코드 → 이름 (표·상세). 사용 안 함 코드도 이름을 보인다. 모르는 코드면 코드 그대로
//   <CodeName groupCode="ORDER_STATUS" code={order.status} />
export function CodeName({ groupCode, code }: { groupCode: string; code: string | null | undefined }) {
  const codes = useCodes(groupCode, { includeDisabled: true }).data
  if (!code) return null
  return <>{codes?.find((c) => c.code === code)?.name ?? code}</>
}
