import type {
  Code,
  CodeCreateRequest,
  CodeEnabledRequest,
  CodeGroup,
  CodeGroupCreateRequest,
  CodeGroupModifyRequest,
  CodeGroupTree,
  CodeGroupOrder,
  CodeModifyRequest,
  CodeOrder,
} from "@/types/code"
import { http } from "./client"

// 공통코드 (code 헬퍼). api.v1.code로 부른다
//   list: 화면용 목록 (로그인한 사용자). 기본은 사용하는 코드만, includeDisabled면 사용 안 함 코드도
//   admin: 공통코드 화면용 (CODE_MANAGE 권한)
// moa: tree·한 그룹 안 순서(order)·한 그룹 안 일괄 사용 여부(setEnabled), 그룹 순서(orderGroups), 그룹 만들기·고치기, 사용 안 함 항목 지우기(deleteCode)를 더했다. 그룹을 지우는 API는 없다 (specs/004-admin-codes FR-008)
const enc = encodeURIComponent
const groupOf = (groupCode: string) => `/api/v1/admin/code-groups/${enc(groupCode)}`
const codesOf = (groupCode: string) => `${groupOf(groupCode)}/codes`

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
    createGroup: (data: CodeGroupCreateRequest) =>
      http.post<CodeGroup>("/api/v1/admin/code-groups", data).then((res) => res.data),
    modifyGroup: (groupCode: string, data: CodeGroupModifyRequest) =>
      http.put<CodeGroup>(groupOf(groupCode), data).then((res) => res.data),
    codes: (groupCode: string) => http.get<Code[]>(codesOf(groupCode)).then((res) => res.data),
    createCode: (groupCode: string, data: CodeCreateRequest) =>
      http.post<Code>(codesOf(groupCode), data).then((res) => res.data),
    modifyCode: (groupCode: string, code: string, data: CodeModifyRequest) =>
      http.put<Code>(`${codesOf(groupCode)}/${enc(code)}`, data).then((res) => res.data),
    // 사용 안 함 항목만 지운다. 사용 중이면 B001 (2026-11-21)
    deleteCode: (groupCode: string, code: string) =>
      http.delete<void>(`${codesOf(groupCode)}/${enc(code)}`).then(() => undefined),
    order: (groupCode: string, orders: CodeOrder[]) =>
      http.put<void>(`${codesOf(groupCode)}/order`, orders).then(() => undefined),
    orderGroups: (orders: CodeGroupOrder[]) =>
      http.put<void>("/api/v1/admin/code-groups/order", orders).then(() => undefined),
    setEnabled: (groupCode: string, data: CodeEnabledRequest) =>
      http.put<void>(`${codesOf(groupCode)}/enabled`, data).then(() => undefined),
  },
}
