import type { Code, CodeCreateRequest, CodeGroup, CodeGroupCreateRequest, CodeModifyRequest } from "@/types/code"
import { http } from "./client"

// 공통코드 (code 헬퍼). api.v1.code로 부른다
//   list: 화면용 목록 (로그인한 사용자). 기본은 사용하는 코드만, includeDisabled면 사용 안 함 코드도
//   admin: 코드 관리 화면용 (CODE_MANAGE 권한)
const enc = encodeURIComponent

export const codeApi = {
  list: (groupCode: string, { includeDisabled = false }: { includeDisabled?: boolean } = {}) =>
    http
      .get<Code[]>(`/api/v1/codes/${enc(groupCode)}`, {
        params: includeDisabled ? { includeDisabled } : undefined,
      })
      .then((res) => res.data),
  admin: {
    groups: () => http.get<CodeGroup[]>("/api/v1/admin/code-groups").then((res) => res.data),
    createGroup: (data: CodeGroupCreateRequest) =>
      http.post<CodeGroup>("/api/v1/admin/code-groups", data).then((res) => res.data),
    codes: (groupCode: string) =>
      http.get<Code[]>(`/api/v1/admin/code-groups/${enc(groupCode)}/codes`).then((res) => res.data),
    createCode: (groupCode: string, data: CodeCreateRequest) =>
      http.post<Code>(`/api/v1/admin/code-groups/${enc(groupCode)}/codes`, data).then((res) => res.data),
    modifyCode: (groupCode: string, code: string, data: CodeModifyRequest) =>
      http.put<Code>(`/api/v1/admin/code-groups/${enc(groupCode)}/codes/${enc(code)}`, data).then((res) => res.data),
    removeCode: (groupCode: string, code: string) =>
      http.delete<void>(`/api/v1/admin/code-groups/${enc(groupCode)}/codes/${enc(code)}`).then(() => undefined),
  },
}
