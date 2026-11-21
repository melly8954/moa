"use client"

import { useTranslations } from "next-intl"
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox"
import { InputGroupAddon } from "@/components/ui/input-group"
import { MENU_ICONS, MenuIcon } from "./icons"

// 메뉴 아이콘 선택 상자 (moa, specs/001-admin-menus FR-010, 2026-11-21)
//   이름으로 찾고, 목록과 입력 칸 앞에 아이콘 미리보기를 보인다. 빈 값("")은 아이콘 없음
const NONE = ""
const NAMES = [NONE, ...Object.keys(MENU_ICONS)]

export function IconPicker({
  id,
  value,
  onChange,
  describedBy,
}: {
  id: string
  value: string
  onChange: (value: string) => void
  describedBy?: string
}) {
  const t = useTranslations("menuAdmin")
  const label = (name: string) => (name === NONE ? t("noIcon") : name)
  return (
    <Combobox items={NAMES} value={value} onValueChange={(next) => onChange(next ?? NONE)} itemToStringLabel={label}>
      <ComboboxInput id={id} placeholder={t("iconSearch")} aria-describedby={describedBy} className="w-full">
        {value !== NONE && (
          <InputGroupAddon align="inline-start">
            <MenuIcon name={value} aria-hidden="true" />
          </InputGroupAddon>
        )}
      </ComboboxInput>
      <ComboboxContent>
        <ComboboxEmpty>{t("iconNotFound")}</ComboboxEmpty>
        <ComboboxList>
          {(name: string) => (
            <ComboboxItem key={name || "none"} value={name}>
              {name === NONE ? (
                <span aria-hidden="true" className="size-4" />
              ) : (
                <MenuIcon name={name} aria-hidden="true" />
              )}
              {label(name)}
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  )
}
