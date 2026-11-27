"use client"

import { useTranslations } from "next-intl"
import { useId } from "react"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import type { ListSearch } from "@/lib/list-search"
import { STATUS_FILTERS } from "@/lib/marketing"

// 상태 거르기 줄 (specs/016-banner-popup FR-008, research R4)
//   키트 ChoiceFilter·SelectFilter는 빈 값을 "전체"로 붙인다. 여기서는 빈 값이 기본 "끝남 빼고"이고 전체는 ALL이라
//   같은 모양(SearchField의 줄)을 따로 그린다. SearchPanel 안에 SearchField 자리로 둔다
export function ExposureStatusFilter({ search }: { search: ListSearch<{ keyword: string; status: string }> }) {
  const t = useTranslations("marketing")
  const id = useId()
  return (
    <div className="flex flex-col sm:flex-row">
      <label
        htmlFor={id}
        className="flex shrink-0 items-center bg-muted px-3 py-2 text-sm font-medium sm:w-(--label-width)"
      >
        {t("status")}
      </label>
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2 px-3 py-2">
        <NativeSelect
          id={id}
          value={search.draft.status ?? ""}
          onChange={(e) => search.submit({ status: e.target.value })}
        >
          {STATUS_FILTERS.map((value) => (
            <NativeSelectOption key={value} value={value}>
              {value === "" ? t("statusNotEnded") : value === "ALL" ? t("statusAll") : t(`statuses.${value}`)}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
    </div>
  )
}
