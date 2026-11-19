import type { Code, CodeCreateRequest, CodeGroup, CodeGroupTree, CodeModifyRequest, CodeOrder } from "@/types/code"
import { http } from "./client"

// 공통코드 (code 헬퍼). api.v1.code로 부른다
//   list: 화면용 목록 (로그인한 사용자). 기본은 사용하는 코드만, includeDisabled면 사용 안 함 코드도
//   admin: 공통코드 화면용 (CODE_MANAGE 권한)
// moa: 그룹은 코드에 등록한다. tree·한 그룹 안 일괄 처리(order·setEnabled·bulkDelete)를 더했다 (specs/004-admin-codes R7)
const enc = encodeURIComponent
const codesOf = (groupCode: string) => `/api/v1/admin/code-groups/${enc(groupCode)}/codes`

export const codeApi = {
  list: (groupCode: string, { includeDisabled = false }: { includeDisabled?: boolean } = {}) =>
    http
      .get<Code[]>(`/api/v1/codes/${enc(groupCode)}`, {
        params: includeDisabled ? { includeDisabled } : undefined,
      })
      .then((res) => res.data),
  admin: {
    groups: () => http.get<CodeGroup[]>("/api/v1/admin/code-groups").then((res) => res.data),
    tree: () => http.get<CodeGroupTree[]>("/api/v1/admin/code-groups/tree").then((res) => res.data),
    codes: (groupCode: string) => http.get<Code[]>(codesOf(groupCode)).then((res) => res.data),
    createCode: (groupCode: string, data: CodeCreateRequest) =>
      http.post<Code>(codesOf(groupCode), data).then((res) => res.data),
    modifyCode: (groupCode: string, code: string, data: CodeModifyRequest) =>
      http.put<Code>(`${codesOf(groupCode)}/${enc(code)}`, data).then((res) => res.data),
    removeCode: (groupCode: string, code: string) =>
      http.delete<void>(`${codesOf(groupCode)}/${enc(code)}`).then(() => undefined),
    order: (groupCode: string, orders: CodeOrder[]) =>
      http.put<void>(`${codesOf(groupCode)}/order`, orders).then(() => undefined),
    setEnabled: (groupCode: string, codes: string[], enabled: boolean) =>
      http.put<void>(`${codesOf(groupCode)}/enabled`, { codes, enabled }).then(() => undefined),
    bulkDelete: (groupCode: string, codes: string[]) =>
      http.post<void>(`${codesOf(groupCode)}/bulk-delete`, { codes }).then(() => undefined),
  },
}
