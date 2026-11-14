"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import {
  ChoiceFilter,
  KeywordFilter,
  PeriodFilter,
  SearchField,
  SearchPanel,
  SearchRow,
} from "@/components/kit/search-panel"
import { StatusText } from "@/components/kit/status-text"
import { formatDateTime } from "@/lib/format"
import { useListSearch } from "@/lib/list-search"
import type { LoginHistory } from "@/types/login-history"

// 접속 이력 (login-history 헬퍼, 권한 LOGIN_HISTORY_READ)
// 조건: 기간·결과·아이디(정확히 일치). 늘 최신순이라 열 정렬은 없다
const DEFAULTS = { from: "", to: "", success: "", loginId: "" }

export default function LoginHistoriesPage() {
  const t = useTranslations("loginHistory")
  const search = useListSearch({ defaults: DEFAULTS })
  const histories = useQuery({
    queryKey: ["admin", "login-histories", search.params],
    queryFn: () => api.v1.loginHistory.getPage(search.params),
    placeholderData: keepPreviousData,
  })

  // 실패 사유는 짧게: 아는 코드는 짧은 이름, 모르는 코드는 "실패"
  const failure = (code?: string | null) =>
    code && t.has(`failure.${code}` as "failure.A006") ? t(`failure.${code}` as "failure.A006") : t("failed")

  const columns: Column<LoginHistory>[] = [
    {
      key: "createdAt",
      header: t("createdAt"),
      kind: "datetime",
      cell: (h) => formatDateTime(h.createdAt, { seconds: true }),
    },
    { key: "loginId", header: t("loginId") },
    {
      key: "success",
      header: t("result"),
      kind: "status",
      cell: (h) =>
        h.success ? (
          <StatusText tone="positive">{t("succeeded")}</StatusText>
        ) : (
          <StatusText tone="negative">{failure(h.failureCode)}</StatusText>
        ),
    },
    { key: "ip", header: t("ip") },
    { key: "device", header: t("device"), cell: (h) => [h.deviceType, h.os, h.browser].join(" · ") },
  ]

  return (
    <ListPage
      title={t("title")}
      description={t("description")}
      search={
        <SearchPanel search={search}>
          <SearchRow>
            <SearchField label={t("period")}>
              <PeriodFilter from="from" to="to" />
            </SearchField>
            <SearchField label={t("result")}>
              <ChoiceFilter
                name="success"
                options={[
                  { value: "true", label: t("succeeded") },
                  { value: "false", label: t("failed") },
                ]}
              />
            </SearchField>
          </SearchRow>
          <SearchField label={t("loginId")}>
            <KeywordFilter name="loginId" placeholder={t("loginIdPlaceholder")} />
          </SearchField>
        </SearchPanel>
      }
    >
      <DataTable caption={t("title")} columns={columns} query={histories} search={search} rowKey={(h) => h.id} />
    </ListPage>
  )
}
