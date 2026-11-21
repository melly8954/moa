"use client"

import { Search } from "lucide-react"
import type { FocusEvent, MouseEvent, ReactNode } from "react"
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox"
import { InputGroupAddon } from "@/components/ui/input-group"

// 글자를 쳐서 찾는 선택 상자 (moa, specs/001-admin-menus FR-010, 2026-11-21)
//   값은 문자열 키다. 입력 칸에는 label(키)를 보이고, filter가 없으면 label로 찾는다
//   renderItem으로 목록 줄을 꾸민다. 입력 칸 앞에는 찾는 칸임을 알리는 돋보기를 둔다 (2026-11-21)
//   칸을 누르거나 닿으면 글자를 모두 골라, 이미 고른 값이 있어도 치는 글자가 그것을 바꿔 바로 찾는다
//   기본 선택 상자(NativeSelect)를 대신한다. 항목이 많아도 이름·경로 등으로 찾아 고른다
export function SearchSelect({
  id,
  items,
  value,
  onChange,
  label,
  filter,
  renderItem,
  placeholder,
  emptyText,
  disabled,
  invalid,
  describedBy,
}: {
  id: string
  items: string[]
  value: string
  onChange: (value: string) => void
  label: (key: string) => string
  filter?: (key: string, query: string) => boolean
  renderItem?: (key: string) => ReactNode
  placeholder?: string
  emptyText: string
  disabled?: boolean
  invalid?: boolean
  describedBy?: string
}) {
  return (
    <Combobox
      items={items}
      value={value}
      // 검색어를 다 지우면 null이 온다. 고른 값은 그대로 둔다 (빈 값은 그 항목을 골라 정한다)
      onValueChange={(next) => next !== null && onChange(next)}
      itemToStringLabel={label}
      filter={filter ? (key: string, query: string) => filter(key, query.trim().toLowerCase()) : undefined}
      disabled={disabled}
    >
      <ComboboxInput
        id={id}
        placeholder={placeholder}
        disabled={disabled}
        aria-invalid={invalid}
        aria-describedby={describedBy}
        className="w-full"
        onFocus={selectAll}
        onClick={selectAll}
      >
        <InputGroupAddon align="inline-start">
          <Search aria-hidden="true" />
        </InputGroupAddon>
      </ComboboxInput>
      <ComboboxContent>
        <ComboboxEmpty>{emptyText}</ComboboxEmpty>
        <ComboboxList>
          {(key: string) => (
            <ComboboxItem key={key || "none"} value={key}>
              {renderItem ? renderItem(key) : label(key)}
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  )
}

// 칸의 글자를 모두 고른다. 이미 고른 값의 이름 뒤에 글자가 붙어 찾지 못하는 일을 막는다
export function selectAll(event: FocusEvent<HTMLInputElement> | MouseEvent<HTMLInputElement>) {
  event.currentTarget.select()
}

// 찾는 글자(소문자)가 어느 한 글에라도 들어 있는가
export function matchesAny(query: string, ...texts: (string | null | undefined)[]) {
  return texts.some((text) => text?.toLowerCase().includes(query))
}
