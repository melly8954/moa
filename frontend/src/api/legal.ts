import type { LegalDocumentKey, LegalDocumentResponse, LegalVersionResponse } from "@/types/legal"
import { http } from "./client"

// moa: 이용약관·개인정보처리방침 (specs/012-terms-privacy contracts/api.md). api.v1.legal로 부른다. 로그인 없이 부른다
//   get: 지금 시행 중인 버전 전문, 시행 예정 버전, 버전 목록
//   version: 버전 하나 (예전·지금·예정). 없으면 404 R001
const base = "/api/v1/legal"

export const legalApi = {
  get: (document: LegalDocumentKey) =>
    http.get<LegalDocumentResponse>(`${base}/${document}`, { skipAuth: true }).then((res) => res.data),
  version: (document: LegalDocumentKey, version: string) =>
    http
      .get<LegalVersionResponse>(`${base}/${document}/versions/${encodeURIComponent(version)}`, { skipAuth: true })
      .then((res) => res.data),
}
