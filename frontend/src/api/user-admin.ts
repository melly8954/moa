import type { PageRequest, PageResponse } from "@/types/api"
import type { Role } from "@/types/user"
import type {
  AdminUser,
  AdminUserCreateRequest,
  AdminUserCreateResponse,
  AdminUserQuery,
  TemporaryPassword,
} from "@/types/user-admin"
import { http } from "./client"

// 사용자 관리 (user-admin 헬퍼, 권한 USER_MANAGE). api.v1.userAdmin으로 부른다
//   늘 최신순이다. 지운 사용자와 시스템 계정은 목록에 없다
const base = "/api/v1/admin/users"

export const userAdminApi = {
  getPage: (params?: AdminUserQuery & PageRequest) =>
    http.get<PageResponse<AdminUser>>(base, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminUser>(`${base}/${id}`).then((res) => res.data),
  create: (data: AdminUserCreateRequest) => http.post<AdminUserCreateResponse>(base, data).then((res) => res.data),
  rename: (id: number, name: string) => http.put<AdminUser>(`${base}/${id}`, { name }).then((res) => res.data),
  changeRole: (id: number, role: Role) => http.put<AdminUser>(`${base}/${id}/role`, { role }).then((res) => res.data),
  lock: (id: number) => http.put<void>(`${base}/${id}/lock`).then(() => undefined),
  unlock: (id: number) => http.delete<void>(`${base}/${id}/lock`).then(() => undefined),
  resetPassword: (id: number) => http.post<TemporaryPassword>(`${base}/${id}/password-reset`).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${base}/${id}`).then(() => undefined),
}
