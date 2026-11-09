"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Plus, Trash2 } from "lucide-react"
import { useState } from "react"
import { ApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import {
  ChoiceFilter,
  KeywordFilter,
  PeriodFilter,
  SearchField,
  SearchPanel,
  SearchRow,
  SelectFilter,
} from "@/components/kit/search-panel"
import { StatusText, type StatusTone } from "@/components/kit/status-text"
import { Button } from "@/components/ui/button"
import { addDays } from "@/lib/date"
import { formatCurrency } from "@/lib/format"
import { type ListSearch, useListSearch } from "@/lib/list-search"
import type { PageResponse } from "@/types/api"

// 개발용 보기 화면이라 글자를 messages에 두지 않는다 (check:i18n은 (dev)를 보지 않는다)
// 프로젝트 화면은 글자를 모두 messages/ko.json 키로 쓴다

type Member = {
  id: number
  name: string
  status: "ACTIVE" | "WAITING" | "STOPPED"
  grade: string
  points: number
  joinedOn: string
  lastSignInAt: string
}

const STATUS: Record<Member["status"], { label: string; tone: StatusTone }> = {
  ACTIVE: { label: "사용", tone: "positive" },
  WAITING: { label: "대기", tone: "neutral" },
  STOPPED: { label: "중지", tone: "negative" },
}
const GRADES = ["브론즈", "실버", "골드", "플래티넘", "다이아", "마스터"]
const NAMES = ["김하늘", "이도윤", "박서연", "최지호", "정예린", "강민준", "조은우", "윤서아", "장하준", "임채원"]

// 가짜 회원 87명 (늘 같은 값)
const MEMBERS: Member[] = Array.from({ length: 87 }, (_, i) => ({
  id: i + 1,
  name: `${NAMES[i % NAMES.length]}${Math.floor(i / NAMES.length) || ""}`,
  status: (["ACTIVE", "ACTIVE", "WAITING", "STOPPED"] as const)[i % 4],
  grade: GRADES[i % GRADES.length],
  points: (i * 7919) % 100_000,
  joinedOn: addDays("2026-11-06", -i * 5),
  lastSignInAt: new Date(Date.UTC(2026, 10, 6, 5, 30) - i * 3_600_000 * 7).toISOString(),
}))

type Filters = { from: string; to: string; status: string; grade: string; keyword: string }
const DEFAULTS: Filters = { from: "", to: "", status: "", grade: "", keyword: "" }

// 가짜 조회: 서버가 하는 일(거르기, 정렬, 쪽 나누기)을 흉내 낸다. 검색어 "error"면 서버 오류
async function fetchMembers(params: ListSearch<Filters>["params"]): Promise<PageResponse<Member>> {
  await new Promise((resolve) => setTimeout(resolve, 150))
  if (params.keyword === "error") {
    throw new ApiError(500, "S001", {
      code: "S001",
      message: "",
      timestamp: "",
      path: "/dev/list",
      traceId: "demo-trace-id",
    })
  }
  let rows = MEMBERS.filter(
    (m) =>
      (!params.from || m.joinedOn >= params.from) &&
      (!params.to || m.joinedOn <= params.to) &&
      (!params.status || m.status === params.status) &&
      (!params.grade || m.grade === params.grade) &&
      (!params.keyword || m.name.includes(params.keyword)),
  )
  if (params.sort) {
    const [field, direction] = params.sort.split(",") as [keyof Member, string]
    rows = [...rows].sort(
      (a, b) => (a[field] > b[field] ? 1 : a[field] < b[field] ? -1 : 0) * (direction === "desc" ? -1 : 1),
    )
  }
  return {
    content: rows.slice(params.page * params.size, (params.page + 1) * params.size),
    page: params.page,
    size: params.size,
    totalElements: rows.length,
    totalPages: Math.ceil(rows.length / params.size),
  }
}

const columns: Column<Member>[] = [
  { key: "id", header: "번호", kind: "number", sortable: true },
  { key: "name", header: "이름", sortable: true },
  { key: "grade", header: "등급" },
  {
    key: "status",
    header: "상태",
    kind: "status",
    cell: (m) => <StatusText tone={STATUS[m.status].tone}>{STATUS[m.status].label}</StatusText>,
  },
  { key: "points", header: "포인트", kind: "number", sortable: true, cell: (m) => formatCurrency(m.points) },
  { key: "joinedOn", header: "가입일", kind: "date", sortable: true },
  { key: "lastSignInAt", header: "마지막 로그인", kind: "datetime" },
]

export function ListDemo() {
  const search = useListSearch({ defaults: DEFAULTS, sort: { field: "id", direction: "desc" } })
  const members = useQuery({
    queryKey: ["dev-members", search.params],
    queryFn: () => fetchMembers(search.params),
    placeholderData: keepPreviousData,
    retry: false,
  })
  const [selected, setSelected] = useState<(string | number)[]>([])

  const removeSelected = async () => {
    if (
      !(await confirm({
        title: `${selected.length}명을 지울까요?`,
        description: "보기 화면이라 실제로 지우지 않아요.",
        destructive: true,
      }))
    )
      return
    setSelected([])
  }

  return (
    <ListPage
      title="회원 목록"
      description="목록 화면 공통 모양 보기 (가짜 데이터)"
      actions={
        <>
          <Button variant="outline" disabled={selected.length === 0} onClick={() => void removeSelected()}>
            <Trash2 />
            선택 삭제
          </Button>
          <Button>
            <Plus />
            등록
          </Button>
        </>
      }
      search={
        <SearchPanel search={search}>
          <SearchRow>
            <SearchField label="가입일">
              <PeriodFilter from="from" to="to" />
            </SearchField>
            <SearchField label="상태">
              <ChoiceFilter
                name="status"
                options={Object.entries(STATUS).map(([value, s]) => ({ value, label: s.label }))}
              />
            </SearchField>
          </SearchRow>
          <SearchRow>
            <SearchField label="등급">
              <SelectFilter name="grade" options={GRADES.map((g) => ({ value: g, label: g }))} />
            </SearchField>
            <SearchField label="이름">
              <KeywordFilter name="keyword" placeholder="이름 일부" />
            </SearchField>
          </SearchRow>
        </SearchPanel>
      }
    >
      <DataTable
        caption="회원 목록"
        columns={columns}
        query={members}
        search={search}
        rowKey={(m) => m.id}
        selection={{ selected, onChange: setSelected }}
      />
    </ListPage>
  )
}
