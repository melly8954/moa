"use client"

import { Combobox as ComboboxPrimitive } from "@base-ui/react"
import { cn } from "cn"
import { Ban, Search } from "lucide-react"
import { useTranslations } from "next-intl"
import { matchesAny, selectAll } from "@/components/kit/search-select"
import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxList } from "@/components/ui/combobox"
import { InputGroupAddon } from "@/components/ui/input-group"
import { MENU_ICONS, MenuIcon } from "./icons"

// 메뉴 아이콘 선택 상자 (moa, specs/001-admin-menus FR-010, 2026-11-21)
//   영어 이름과 한글 이름·비슷한 말(messages menuAdmin.iconNames·iconKeywords)로 찾는다
//   목록은 아이콘과 한글 이름을 칸(격자)으로 보이고, 방향키로 위·아래·옆 칸으로 옮긴다. 첫 칸 빈 값("")은 아이콘 없음
const NONE = ""
const NAMES = [NONE, ...Object.keys(MENU_ICONS)]
const COLUMNS = 4

type IconKey = "iconNames.home"

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
  const label = (name: string) => (name === NONE ? t("noIcon") : t(`iconNames.${name}` as IconKey))
  const keywords = (name: string) => (name === NONE ? "" : t(`iconKeywords.${name}` as "iconKeywords.home"))
  return (
    <Combobox
      items={NAMES}
      value={value}
      // 검색어를 다 지우면 null이 온다. 고른 값은 그대로 둔다 ("없음"은 첫 칸을 골라 정한다)
      onValueChange={(next) => next !== null && onChange(next)}
      itemToStringLabel={label}
      filter={(name: string, query: string) => {
        const q = query.trim().toLowerCase()
        return matchesAny(q, name, label(name), keywords(name))
      }}
      grid
    >
      {/* 고른 아이콘이 있으면 그 미리보기, 없으면 찾는 칸임을 알리는 돋보기. 칸을 누르면 글자를 모두 골라 바로 찾는다 */}
      <ComboboxInput
        id={id}
        placeholder={t("iconSearch")}
        aria-describedby={describedBy}
        className="w-full"
        onFocus={selectAll}
        onClick={selectAll}
      >
        <InputGroupAddon align="inline-start">
          {value !== NONE ? <MenuIcon name={value} aria-hidden="true" /> : <Search aria-hidden="true" />}
        </InputGroupAddon>
      </ComboboxInput>
      <ComboboxContent>
        <ComboboxEmpty>{t("iconNotFound")}</ComboboxEmpty>
        <ComboboxList>
          <IconGrid label={label} />
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  )
}

// 찾은 아이콘을 한 줄에 COLUMNS개씩 나눠 칸으로 보인다 (grid는 줄(Row)을 보고 방향키를 옮긴다)
function IconGrid({ label }: { label: (name: string) => string }) {
  const names = ComboboxPrimitive.useFilteredItems<string>()
  const rows: string[][] = []
  for (let i = 0; i < names.length; i += COLUMNS) rows.push(names.slice(i, i + COLUMNS))
  return rows.map((row) => (
    <ComboboxPrimitive.Row key={row.join("|")} className="grid grid-cols-4 gap-1">
      {row.map((name) => (
        <ComboboxPrimitive.Item
          key={name || "none"}
          value={name}
          className={cn(
            "flex cursor-default flex-col items-center gap-1 rounded-md px-1 py-2 text-xs text-muted-foreground outline-hidden select-none",
            "data-highlighted:bg-accent data-highlighted:text-accent-foreground data-selected:ring-1 data-selected:ring-ring",
          )}
        >
          {name === NONE ? (
            <Ban aria-hidden="true" className="size-5" />
          ) : (
            <MenuIcon name={name} aria-hidden="true" className="size-5 text-foreground" />
          )}
          <span className="max-w-full truncate">{label(name)}</span>
        </ComboboxPrimitive.Item>
      ))}
    </ComboboxPrimitive.Row>
  ))
}
