import type { Permission, Role, RoleCreateRequest, RoleModifyRequest } from "@/types/role"
import { http } from "./client"

// 역할·권한 (rbac 헬퍼, 권한 ROLE_MANAGE). api.v1.role로 부른다
//   내가 가진 권한 밖을 다루면 서버가 A005로 막는다 (권한 상승 방지)
export const roleApi = {
  permissions: () => http.get<Permission[]>("/api/v1/admin/permissions").then((res) => res.data),
  list: () => http.get<Role[]>("/api/v1/admin/roles").then((res) => res.data),
  get: (id: number) => http.get<Role>(`/api/v1/admin/roles/${id}`).then((res) => res.data),
  create: (data: RoleCreateRequest) => http.post<Role>("/api/v1/admin/roles", data).then((res) => res.data),
  modify: (id: number, data: RoleModifyRequest) =>
    http.put<Role>(`/api/v1/admin/roles/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`/api/v1/admin/roles/${id}`).then(() => undefined),
  // 사용자에게 준 역할. 바꿀 때는 통째로 (빈 목록이면 모두 뺀다)
  userRoles: (userId: number) => http.get<Role[]>(`/api/v1/admin/users/${userId}/roles`).then((res) => res.data),
  replaceUserRoles: (userId: number, roleIds: number[]) =>
    http.put<void>(`/api/v1/admin/users/${userId}/roles`, { roleIds }).then(() => undefined),
}
