"use client"

import { useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { LinkButton } from "@/components/kit/link-button"
import { QueryState } from "@/components/kit/query-state"
import { legalPath } from "@/lib/legal"
import type { LegalDocumentKey } from "@/types/legal"
import { LegalDocument } from "./legal-document"
import { LegalNotice } from "./legal-notice"
import { LegalVersions } from "./legal-versions"

export const legalQueryKey = (document: LegalDocumentKey) => ["legal", document] as const

// 문서 화면 /terms, /privacy (specs/012-terms-privacy US1, US3, contracts/screens.md)
//   지금 시행 중인 버전. 예정 버전이 있으면 위에 안내 띠, 제목 아래에 버전 목록
export function LegalPage({ document }: { document: LegalDocumentKey }) {
  const query = useQuery({ queryKey: legalQueryKey(document), queryFn: () => api.v1.legal.get(document) })
  return (
    <QueryState query={query}>
      {(data) => (
        <div className="flex flex-col gap-4">
          {data.upcoming && (
            <LegalNotice
              document={document}
              notice={{ kind: "upcoming", version: data.upcoming.version, effectiveDate: data.upcoming.effectiveDate }}
            />
          )}
          <LegalDocument
            content={data.current}
            afterMeta={<LegalVersions document={document} versions={data.versions} viewing={data.current.version} />}
          />
        </div>
      )}
    </QueryState>
  )
}

// 버전 화면 /terms/versions/{version}, /privacy/versions/{version} (US3-AC2·AC3)
//   지금 버전이 아니면 위에 안내 띠. 없는 버전(R001)이면 "없는 버전이에요"와 지금 버전 링크
export function LegalVersionPage({ document, version }: { document: LegalDocumentKey; version: string }) {
  const t = useTranslations("legal")
  const query = useQuery({
    queryKey: [...legalQueryKey(document), "versions", version],
    queryFn: () => api.v1.legal.version(document, version),
    retry: (count, error) => toApiError(error).code !== "R001" && count < 2,
  })
  // 버전 목록은 지금 문서 응답에 있다 (같은 키라 문서 화면에서 왔으면 다시 부르지 않는다)
  const list = useQuery({ queryKey: legalQueryKey(document), queryFn: () => api.v1.legal.get(document) })
  if (query.isError && toApiError(query.error).code === "R001") {
    return (
      <div className="flex flex-col items-start gap-3">
        <h1 className="text-xl font-semibold">{t("versionNotFound")}</h1>
        <LinkButton href={legalPath(document)} variant="outline">
          {t("viewCurrent")}
        </LinkButton>
      </div>
    )
  }
  return (
    <QueryState query={query}>
      {(data) => (
        <div className="flex flex-col gap-4">
          {data.state === "PAST" && <LegalNotice document={document} notice={{ kind: "past" }} />}
          {data.state === "UPCOMING" && (
            <LegalNotice document={document} notice={{ kind: "future", effectiveDate: data.effectiveDate }} />
          )}
          <LegalDocument
            content={data}
            afterMeta={
              list.data && <LegalVersions document={document} versions={list.data.versions} viewing={data.version} />
            }
          />
        </div>
      )}
    </QueryState>
  )
}
