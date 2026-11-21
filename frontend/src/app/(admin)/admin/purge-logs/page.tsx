"use client"

import Link from "next/link"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import { ChoiceFilter, PeriodFilter, SearchField, SearchPanel } from "@/components/kit/search-panel"
import { formatDateTime } from "@/lib/format"
import { useListSearch } from "@/lib/list-search"
import type { PurgeLog, PurgeTarget } from "@/types/purge-log"

// 개인정보 파기 기록 (moa, specs/013-purge-log US2, 권한 PURGE_LOG_READ)
// 조건: 기간·대상 종류. 처음 기간은 접속 이력 화면과 같다(FR-009). 늘 최신순이라 열 정렬은 없다. 보기만 한다
const DEFAULTS = { from: "", to: "", target: "" }
const TARGETS: PurgeTarget[] = ["LOGIN_HISTORY", "NOTIFICATION", "REFRESH_TOKEN", "RICH_TEXT_IMAGE"]

export default function PurgeLogsPage() {
  const t = useTranslations("purgeLog")
  const search = useListSearch({ defaults: DEFAULTS })
  const logs = useQuery({
    queryKey: ["admin", "purge-logs", search.params],
    queryFn: () => api.v1.purgeLog.getPage(search.params),
    placeholderData: keepPreviousData,
  })

  const columns: Column<PurgeLog>[] = [
    {
      key: "createdAt",
      header: t("createdAt"),
      kind: "datetime",
      cell: (l) => formatDateTime(l.createdAt, { seconds: true }),
    },
    { key: "target", header: t("target"), cell: (l) => t(`targets.${l.target}`) },
    { key: "count", header: t("count"), kind: "number" },
    { key: "method", header: t("method"), cell: (l) => t(`methods.${l.method}`) },
    { key: "actor", header: t("actor"), cell: (l) => t(`actors.${l.actor}`) },
  ]

  return (
    <ListPage
      title={t("title")}
      description={t.rich("description", { link: (chunks) => <Link href="/admin/login-histories">{chunks}</Link> })}
      search={
        <SearchPanel search={search}>
          {/* 기간은 빠른 단추와 날짜 칸이 한 줄에 놓이게 한 줄을 다 쓴다 (011 FR-010) */}
          <SearchField label={t("period")}>
            <PeriodFilter from="from" to="to" />
          </SearchField>
          <SearchField label={t("target")}>
            <ChoiceFilter
              name="target"
              options={TARGETS.map((target) => ({ value: target, label: t(`targets.${target}`) }))}
            />
          </SearchField>
        </SearchPanel>
      }
    >
      <DataTable caption={t("title")} columns={columns} query={logs} search={search} rowKey={(l) => l.id} />
    </ListPage>
  )
}
