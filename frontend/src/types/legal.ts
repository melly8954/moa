// moa: 공개 문서 API의 응답 (specs/012-terms-privacy contracts/api.md)
// 날짜는 한국 날짜 YYYY-MM-DD 문자열이다

export type LegalDocumentKey = "terms" | "privacy"

export const LEGAL_DOCUMENTS: LegalDocumentKey[] = ["terms", "privacy"]

// 오늘 기준 상태. 지금 시행 중, 예전, 시행 예정
export type VersionState = "CURRENT" | "PAST" | "UPCOMING"

export type LegalSection = {
  id: string
  heading: string
  html: string
}

export type LegalVersionContent = {
  version: string
  effectiveDate: string
  title: string
  sections: LegalSection[]
}

// GET /api/v1/legal/{document}
export type LegalDocumentResponse = {
  document: LegalDocumentKey
  current: LegalVersionContent
  upcoming: { version: string; effectiveDate: string } | null
  versions: { version: string; effectiveDate: string; state: VersionState }[]
}

// GET /api/v1/legal/{document}/versions/{version}
export type LegalVersionResponse = LegalVersionContent & {
  document: LegalDocumentKey
  state: VersionState
  currentVersion: string
}
