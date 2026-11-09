"use client"

import type { UseQueryResult } from "@tanstack/react-query"
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight } from "lucide-react"
import { useTranslations } from "next-intl"
import { Alert, AlertAction, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Empty, EmptyDescription, EmptyHeader } from "@/components/ui/empty"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Skeleton } from "@/components/ui/skeleton"
import { TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDate, formatDateTime, formatNumber } from "@/lib/format"
import { type ListSearch, PAGE_SIZES, type PageSize } from "@/lib/list-search"
import { cn } from "@/lib/utils"
import type { PageResponse } from "@/types/api"

// 목록 표: 서버가 나눈 쪽(PageResponse)을 그린다. 쪽 번호, 서버 정렬, 쪽 크기(10·20·50), 행 선택
//   const search = useListSearch({ defaults: { keyword: "" } })
//   const orders = useQuery({ queryKey: ["orders", search.params], queryFn: () => api.v1.order.getPage(search.params),
//                             placeholderData: keepPreviousData })
//   <DataTable columns={columns} query={orders} search={search} rowKey={(o) => o.id} />
// 쪽·정렬·쪽 크기는 search가 주소에 둔다 (lib/list-search.ts)
//
// 열 폭 규칙 (kind): 폭은 layout-tokens.css의 --col-* 변수다
//   date 날짜 / datetime 시각(연도까지) / number 숫자(오른쪽 정렬) / status 상태 / actions 버튼 칸
//   text(기본) 내용만큼 넓어지고 --col-text-max를 넘으면 말줄임
// 셀을 따로 그리지 않으면 kind에 맞춰 형식을 입힌다 (date·datetime·number)
// 좁은 화면에서는 표 안에서 가로로 넘긴다 (화면 전체는 넘치지 않는다)

export type ColumnKind = "text" | "number" | "date" | "datetime" | "status" | "actions"

export type Column<T> = {
  // 행에서 값을 꺼낼 칸 이름이자 정렬 필드 이름 (서버 정렬 "필드,asc")
  key: string
  header: string
  kind?: ColumnKind
  sortable?: boolean
  cell?: (row: T) => React.ReactNode
  className?: string
}

type Id = string | number

const WIDTH: Record<ColumnKind, string> = {
  text: "",
  number: "w-(--col-number) text-right",
  date: "w-(--col-date)",
  datetime: "w-(--col-datetime)",
  status: "w-(--col-status)",
  actions: "w-(--col-actions) text-right",
}

function cellContent<T>(column: Column<T>, row: T): React.ReactNode {
  return column.cell
    ? column.cell(row)
    : defaultCell(column.kind ?? "text", (row as Record<string, unknown>)[column.key])
}

function defaultCell(kind: ColumnKind, value: unknown): React.ReactNode {
  if (value === null || value === undefined) return ""
  if (kind === "date") return formatDate(value as string)
  if (kind === "datetime") return formatDateTime(value as string)
  if (kind === "number" && typeof value === "number") return formatNumber(value)
  return String(value)
}

export function DataTable<T>({
  columns,
  query,
  search,
  rowKey,
  selection,
  caption,
}: {
  columns: Column<T>[]
  query: Pick<UseQueryResult<PageResponse<T>>, "data" | "error" | "isPending" | "isError" | "refetch">
  search: Pick<
    ListSearch<Record<string, string>>,
    "page" | "size" | "sort" | "isFiltered" | "setPage" | "setSize" | "setSort"
  >
  rowKey: (row: T) => Id
  // 행 선택: 고른 id 목록과 바꾸는 함수. 주면 맨 앞에 선택 칸이 생긴다 (이 쪽 안에서만 고른다)
  selection?: { selected: Id[]; onChange: (selected: Id[]) => void }
  // 표 이름 (화면 낭독기). 보통 화면 제목
  caption?: string
}) {
  const t = useTranslations("table")
  const { describeError } = useErrorMessages()
  const rows = query.data?.content ?? []
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)
  const colSpan = columns.length + (selection ? 1 : 0)

  const rowIds = rows.map(rowKey)
  const selectedOnPage = selection ? rowIds.filter((id) => selection.selected.includes(id)) : []
  const allSelected = rows.length > 0 && selectedOnPage.length === rows.length

  const toggleSort = (key: string) => {
    const current = search.sort?.field === key ? search.sort.direction : undefined
    // 오름차순 → 내림차순 → 정렬 없음
    search.setSort(
      current === undefined
        ? { field: key, direction: "asc" }
        : current === "asc"
          ? { field: key, direction: "desc" }
          : undefined,
    )
  }

  let body: React.ReactNode
  if (query.isPending) {
    body = Array.from({ length: Math.min(search.size, 10) }, (_, i) => (
      <TableRow key={i}>
        <TableCell colSpan={colSpan}>
          <Skeleton className="h-5 w-full" />
        </TableCell>
      </TableRow>
    ))
  } else if (query.isError) {
    const { message, traceId } = describeError(query.error)
    body = (
      <TableRow>
        <TableCell colSpan={colSpan} className="whitespace-normal">
          <Alert variant="destructive" role="alert">
            <AlertDescription>
              {message}
              {traceId && <span className="block text-xs">{t("traceId", { traceId })}</span>}
            </AlertDescription>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => void query.refetch()}>
                {t("retry")}
              </Button>
            </AlertAction>
          </Alert>
        </TableCell>
      </TableRow>
    )
  } else if (rows.length === 0) {
    body = (
      <TableRow>
        <TableCell colSpan={colSpan} className="whitespace-normal">
          <Empty>
            <EmptyHeader>
              <EmptyDescription>{search.isFiltered ? t("noResults") : t("empty")}</EmptyDescription>
            </EmptyHeader>
          </Empty>
        </TableCell>
      </TableRow>
    )
  } else {
    body = rows.map((row, index) => {
      const id = rowIds[index]
      const selected = selection?.selected.includes(id) ?? false
      return (
        <TableRow key={id} data-state={selected ? "selected" : undefined}>
          {selection && (
            <TableCell className="w-(--col-select)">
              <Checkbox
                aria-label={t("selectRow", { row: String(id) })}
                checked={selected}
                onCheckedChange={(checked) =>
                  selection.onChange(checked ? [...selection.selected, id] : selection.selected.filter((s) => s !== id))
                }
              />
            </TableCell>
          )}
          {columns.map((column) => {
            const kind = column.kind ?? "text"
            return (
              <TableCell key={column.key} className={cn(WIDTH[kind], column.className)}>
                {kind === "text" ? (
                  <div className="max-w-(--col-text-max) truncate">{cellContent(column, row)}</div>
                ) : (
                  cellContent(column, row)
                )}
              </TableCell>
            )
          })}
        </TableRow>
      )
    })
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="rounded-lg border">
        {/* 넓은 표는 이 상자 안에서 가로로 넘긴다. 키보드로도 넘길 수 있게 초점을 받는다 (shadcn Table 대신 직접 둔다) */}
        <div
          role="region"
          aria-label={caption}
          tabIndex={0}
          className="relative w-full overflow-x-auto rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <table data-slot="table" className="w-full caption-bottom text-sm">
            {caption && <caption className="sr-only">{caption}</caption>}
            <TableHeader>
              <TableRow>
                {selection && (
                  <TableHead className="w-(--col-select)">
                    <Checkbox
                      aria-label={t("selectAll")}
                      checked={allSelected}
                      indeterminate={selectedOnPage.length > 0 && !allSelected}
                      disabled={rows.length === 0}
                      onCheckedChange={(checked) =>
                        selection.onChange(
                          checked
                            ? [...new Set([...selection.selected, ...rowIds])]
                            : selection.selected.filter((s) => !rowIds.includes(s)),
                        )
                      }
                    />
                  </TableHead>
                )}
                {columns.map((column) => {
                  const kind = column.kind ?? "text"
                  const direction = search.sort?.field === column.key ? search.sort.direction : undefined
                  const SortIcon = direction === "asc" ? ArrowUp : direction === "desc" ? ArrowDown : ArrowUpDown
                  return (
                    <TableHead
                      key={column.key}
                      className={cn(WIDTH[kind], column.className)}
                      aria-sort={direction === "asc" ? "ascending" : direction === "desc" ? "descending" : undefined}
                    >
                      {column.sortable ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className={cn("-mx-2", kind === "number" && "flex-row-reverse")}
                          onClick={() => toggleSort(column.key)}
                        >
                          {column.header}
                          <SortIcon className={cn(!direction && "text-muted-foreground")} />
                        </Button>
                      ) : (
                        column.header
                      )}
                    </TableHead>
                  )
                })}
              </TableRow>
            </TableHeader>
            <TableBody>{body}</TableBody>
          </table>
        </div>
      </div>
      <Pager
        page={search.page}
        totalPages={totalPages}
        total={query.data?.totalElements}
        size={search.size}
        onPage={search.setPage}
        onSize={search.setSize}
      />
    </div>
  )
}

// 쪽 이동: 총 건수, 쪽 크기, 이전·번호(최대 5개)·다음
function Pager({
  page,
  totalPages,
  total,
  size,
  onPage,
  onSize,
}: {
  page: number
  totalPages: number
  total?: number
  size: PageSize
  onPage: (page: number) => void
  onSize: (size: PageSize) => void
}) {
  const t = useTranslations("table")
  const first = Math.max(1, Math.min(page - 2, totalPages - 4))
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => first + i)
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
      <div className="flex items-center gap-2 text-muted-foreground">
        {total !== undefined && <span>{t("total", { count: formatNumber(total) })}</span>}
        <NativeSelect
          size="sm"
          aria-label={t("pageSize")}
          value={String(size)}
          onChange={(e) => onSize(Number(e.target.value) as PageSize)}
        >
          {PAGE_SIZES.map((option) => (
            <NativeSelectOption key={option} value={option}>
              {t("perPage", { size: option })}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      <nav aria-label={t("pagination")} className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t("previous")}
          disabled={page <= 1}
          onClick={() => onPage(page - 1)}
        >
          <ChevronLeft />
        </Button>
        {pages.map((p) => (
          <Button
            key={p}
            variant={p === page ? "outline" : "ghost"}
            size="icon-sm"
            aria-label={t("page", { page: p })}
            aria-current={p === page ? "page" : undefined}
            onClick={() => onPage(p)}
          >
            {p}
          </Button>
        ))}
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t("next")}
          disabled={page >= totalPages}
          onClick={() => onPage(page + 1)}
        >
          <ChevronRight />
        </Button>
      </nav>
    </div>
  )
}
