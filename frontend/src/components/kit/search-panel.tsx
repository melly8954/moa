"use client"

import { RotateCcw, Search } from "lucide-react"
import { useTranslations } from "next-intl"
import { createContext, useContext, useId } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import type { ListSearch } from "@/lib/list-search"
import { cn } from "@/lib/utils"
import { DateRangePicker } from "./date-picker"

// 목록 화면 검색 영역: 라벨 열 조건표
//   <SearchPanel search={search}>
//     <SearchRow>
//       <SearchField label={t("period")}><PeriodFilter from="from" to="to" /></SearchField>
//       <SearchField label={t("status")}><ChoiceFilter name="status" options={statusOptions} /></SearchField>
//     </SearchRow>
//     <SearchField label={t("keyword")}><KeywordFilter name="keyword" /></SearchField>
//   </SearchPanel>
// 조건은 search.draft에 쌓이고 조회(또는 검색어 칸에서 Enter)를 눌러야 반영된다 (lib/list-search.ts)
// 어떤 조건을 고를지
//   Period: 기간 / Choice: 선택지 2~4개 (누름 단추) / Select: 선택지 5개 이상 (선택 상자) / Keyword: 검색어
// 한 줄에 조건 둘까지 (SearchRow). 좁은 화면에서는 한 줄에 하나씩 쌓인다

export type Option = { value: string; label: string }

type AnySearch = ListSearch<Record<string, string>>

const SearchContext = createContext<AnySearch | null>(null)
const FieldContext = createContext<{ inputId: string; labelId: string; label: string } | null>(null)

function useSearch() {
  const search = useContext(SearchContext)
  if (!search) throw new Error("Search filters must be inside <SearchPanel>")
  return search
}

function useField() {
  const field = useContext(FieldContext)
  if (!field) throw new Error("Search filters must be inside <SearchField>")
  return field
}

export function SearchPanel<F extends Record<string, string>>({
  search,
  children,
  className,
}: {
  search: ListSearch<F>
  children: React.ReactNode
  className?: string
}) {
  const t = useTranslations("search")
  return (
    <SearchContext.Provider value={search as unknown as AnySearch}>
      <form
        role="search"
        aria-label={t("panel")}
        noValidate
        className={cn("flex flex-col gap-3", className)}
        onSubmit={(e) => {
          e.preventDefault()
          search.submit()
        }}
      >
        <div className="divide-y overflow-hidden rounded-lg border">{children}</div>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={search.reset}>
            <RotateCcw />
            {t("reset")}
          </Button>
          <Button type="submit">
            <Search />
            {t("submit")}
          </Button>
        </div>
      </form>
    </SearchContext.Provider>
  )
}

// 조건 둘을 한 줄에 (넓은 화면)
export function SearchRow({ children }: { children: React.ReactNode }) {
  return <div className="grid divide-y md:grid-cols-2 md:divide-x md:divide-y-0">{children}</div>
}

// 조건 한 칸: 왼쪽 라벨 칸(폭 --label-width) + 오른쪽 입력
export function SearchField({ label, children }: { label: string; children: React.ReactNode }) {
  const inputId = useId()
  const labelId = useId()
  return (
    <FieldContext.Provider value={{ inputId, labelId, label }}>
      <div className="flex flex-col sm:flex-row">
        <label
          id={labelId}
          htmlFor={inputId}
          className="flex shrink-0 items-center bg-muted px-3 py-2 text-sm font-medium sm:w-(--label-width)"
        >
          {label}
        </label>
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2 px-3 py-2">{children}</div>
      </div>
    </FieldContext.Provider>
  )
}

// 검색어. Enter로 조회한다
export function KeywordFilter({ name, placeholder }: { name: string; placeholder?: string }) {
  const search = useSearch()
  const { inputId } = useField()
  return (
    <Input
      id={inputId}
      type="search"
      className="w-full max-w-sm"
      placeholder={placeholder}
      value={search.draft[name] ?? ""}
      onChange={(e) => search.setDraft({ [name]: e.target.value })}
    />
  )
}

// 선택지 2~4개. "전체"(빈 값)가 맨 앞에 붙는다
export function ChoiceFilter({ name, options }: { name: string; options: Option[] }) {
  const t = useTranslations("search")
  const search = useSearch()
  const { labelId } = useField()
  const value = search.draft[name] ?? ""
  return (
    <ToggleGroup
      aria-labelledby={labelId}
      variant="outline"
      size="sm"
      value={[value]}
      onValueChange={(next: string[]) => search.setDraft({ [name]: next[0] ?? "" })}
    >
      {[{ value: "", label: t("all") }, ...options].map((option) => (
        <ToggleGroupItem key={option.value} value={option.value}>
          {option.label}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  )
}

// 선택지 5개 이상. "전체"(빈 값)가 맨 앞에 붙는다
export function SelectFilter({ name, options }: { name: string; options: Option[] }) {
  const t = useTranslations("search")
  const search = useSearch()
  const { inputId } = useField()
  return (
    <NativeSelect
      id={inputId}
      value={search.draft[name] ?? ""}
      onChange={(e) => search.setDraft({ [name]: e.target.value })}
    >
      <NativeSelectOption value="">{t("all")}</NativeSelectOption>
      {options.map((option) => (
        <NativeSelectOption key={option.value} value={option.value}>
          {option.label}
        </NativeSelectOption>
      ))}
    </NativeSelect>
  )
}

// 기간. from·to는 조건 이름 (예: from="from" to="to")
export function PeriodFilter({ from, to, min, max }: { from: string; to: string; min?: string; max?: string }) {
  const t = useTranslations("datePicker")
  const search = useSearch()
  const { label } = useField()
  return (
    <DateRangePicker
      value={{ from: search.draft[from] ?? "", to: search.draft[to] ?? "" }}
      onChange={(range) => search.setDraft({ [from]: range.from, [to]: range.to })}
      min={min}
      max={max}
      fromLabel={`${label} ${t("from")}`}
      toLabel={`${label} ${t("to")}`}
    />
  )
}
