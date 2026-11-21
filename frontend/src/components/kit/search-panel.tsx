"use client"

import { RotateCcw } from "lucide-react"
import { useTranslations } from "next-intl"
import { createContext, useContext, useEffect, useId, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import type { ListSearch } from "@/lib/list-search"
import { cn } from "@/lib/utils"
import { DateRangePicker } from "./date-picker"
import { SegmentedButtons } from "./segmented-buttons"

// 목록 화면 검색 영역: 라벨 열 조건표
//   <SearchPanel search={search}>
//     <SearchRow>
//       <SearchField label={t("period")}><PeriodFilter from="from" to="to" /></SearchField>
//       <SearchField label={t("status")}><ChoiceFilter name="status" options={statusOptions} /></SearchField>
//     </SearchRow>
//     <SearchField label={t("keyword")}><KeywordFilter name="keyword" /></SearchField>
//   </SearchPanel>
// moa: 조회 단추가 없다. 고르기·기간·선택 상자는 바꾸는 즉시, 검색어는 엔터나 0.5초 멈춤 뒤 조회한다 (lib/list-search.ts, specs/011-list-screen FR-012·FR-013)
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
        // 조건마다 스스로 조회하므로 폼 제출(칸에서 엔터)은 막기만 한다
        onSubmit={(e) => e.preventDefault()}
      >
        {/* moa: 조건 둘을 한 줄에 두는지는 화면 폭이 아니라 이 영역의 폭으로 정한다 (container query, specs/011-list-screen Edge Cases) */}
        <div className="@container/search divide-y overflow-hidden rounded-lg border">{children}</div>
        {/* moa: 조회 단추는 없다. 조건을 바꾸면 바로 조회한다 (specs/011-list-screen FR-012, 2026-11-22) */}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={search.reset}>
            <RotateCcw />
            {t("reset")}
          </Button>
        </div>
      </form>
    </SearchContext.Provider>
  )
}

// 조건 둘을 한 줄에 (검색 영역이 넓을 때, 약 672px 이상)
// moa: 화면 폭(md) 기준이면 사이드바 때문에 본문이 좁은 768px 화면에서 한 칸이 좁아 조건이 옆 칸으로 넘쳤다
export function SearchRow({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid divide-y @2xl/search:grid-cols-2 @2xl/search:divide-x @2xl/search:divide-y-0">{children}</div>
  )
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

// 검색어. 엔터는 바로, 치기를 멈추고 0.5초 뒤 조회한다 (moa, specs/011-list-screen FR-013, 2026-11-22)
//   한글 조합 중에는 기다린다. 0.5초 뒤 조회는 방문 기록을 새로 쌓지 않는다(replace), 엔터는 쌓는다
const KEYWORD_DELAY = 500

export function KeywordFilter({ name, placeholder }: { name: string; placeholder?: string }) {
  const search = useSearch()
  const { inputId } = useField()
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)
  const composing = useRef(false)
  const value = search.draft[name] ?? ""
  useEffect(() => () => clearTimeout(timer.current), [])
  const later = (next: string) => {
    clearTimeout(timer.current)
    timer.current = setTimeout(() => {
      if (next !== (search.filters[name] ?? "")) search.submit({ [name]: next }, { replace: true })
    }, KEYWORD_DELAY)
  }
  return (
    <Input
      id={inputId}
      type="search"
      className="w-full max-w-sm"
      placeholder={placeholder}
      value={value}
      onChange={(e) => {
        search.setDraft({ [name]: e.target.value })
        if (!composing.current) later(e.target.value)
      }}
      onCompositionStart={() => {
        composing.current = true
        clearTimeout(timer.current)
      }}
      onCompositionEnd={(e) => {
        composing.current = false
        later(e.currentTarget.value)
      }}
      onKeyDown={(e) => {
        if (e.key !== "Enter" || e.nativeEvent.isComposing) return
        e.preventDefault()
        clearTimeout(timer.current)
        search.submit({ [name]: e.currentTarget.value })
      }}
    />
  )
}

// 선택지 2~4개. "전체"(빈 값)가 맨 앞에 붙는다
// moa: 기간 빠른 단추와 같은 나눔 단추로 보인다 (specs/011-list-screen FR-010, 2026-11-21)
//   예전 base-ui ToggleGroup은 빈 값을 자동 id로 바꿔, "전체"를 누르면 조건에 base-ui-… 값이 들어가 조회가 V001로 막혔다
export function ChoiceFilter({ name, options }: { name: string; options: Option[] }) {
  const t = useTranslations("search")
  const search = useSearch()
  const { labelId } = useField()
  return (
    <SegmentedButtons
      aria-labelledby={labelId}
      items={[{ value: "", label: t("all") }, ...options]}
      value={search.draft[name] ?? ""}
      onChange={(value) => search.submit({ [name]: value })}
    />
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
      onChange={(e) => search.submit({ [name]: e.target.value })}
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
      // 기간이 정해지면 바로 조회한다. 시작이 끝보다 늦으면 조회하지 않는다 (FR-012)
      onSettle={(range) => {
        if (range.from !== "" && range.to !== "" && range.from > range.to) return
        search.submit({ [from]: range.from, [to]: range.to })
      }}
      min={min}
      max={max}
      fromLabel={`${label} ${t("from")}`}
      toLabel={`${label} ${t("to")}`}
    />
  )
}
