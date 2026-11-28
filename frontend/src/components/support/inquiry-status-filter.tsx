"use client"

import { useTranslations } from "next-intl"
import { SegmentedButtons } from "@/components/kit/segmented-buttons"
import type { ListSearch } from "@/lib/list-search"

// 문의 관리의 상태 거르기 (specs/017-customer-center research R6)
//   키트 ChoiceFilter는 빈 값을 "전체"로 붙인다. 여기서는 빈 값이 기본 "답변 대기"이고 전체는 ALL이라
//   같은 모양(SearchField 줄)을 따로 그린다. SearchPanel 안에 SearchField 자리로 둔다 (016 ExposureStatusFilter와 같은 방식)
const VALUES = ["", "ANSWERED", "ALL"] as const

type StatusValue = (typeof VALUES)[number]

export function InquiryStatusFilter<F extends { status: string }>({ search }: { search: ListSearch<F> }) {
  const t = useTranslations("inquiry")
  const labelId = "inquiry-status-filter-label"
  return (
    <div className="flex flex-col sm:flex-row">
      <span
        id={labelId}
        className="flex shrink-0 items-center bg-muted px-3 py-2 text-sm font-medium sm:w-(--label-width)"
      >
        {t("status")}
      </span>
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2 px-3 py-2">
        <SegmentedButtons
          aria-labelledby={labelId}
          items={VALUES.map((value) => ({
            value,
            label: value === "" ? t("statuses.WAITING") : value === "ALL" ? t("statusAll") : t(`statuses.${value}`),
          }))}
          value={(search.draft.status ?? "") as StatusValue}
          onChange={(value) => search.submit({ status: value } as Partial<F>)}
        />
      </div>
    </div>
  )
}
