import type { PageRequest, PageResponse } from "@/types/api"
import type {
  Permission,
  PermissionTree,
  Role,
  RoleCreateRequest,
  RoleModifyRequest,
  RoleUser,
  RoleUserQuery,
} from "@/types/role"
import { http } from "./client"

// 역할·권한 (rbac 헬퍼, 권한 ROLE_MANAGE). api.v1.role로 부른다
//   내가 가진 권한 밖을 다루면 서버가 A005로 막는다 (권한 상승 방지)
// moa: 권한 트리·사용자별 역할·구성원 (specs/003-admin-roles FR-004~FR-006). 사용자 관리 권한 없이 역할 관리 권한으로 쓴다
export const roleApi = {
  permissions: () => http.get<Permission[]>("/api/v1/admin/permissions").then((res) => res.data),
  permissionTree: () => http.get<PermissionTree>("/api/v1/admin/roles/permission-tree").then((res) => res.data),
  list: () => http.get<Role[]>("/api/v1/admin/roles").then((res) => res.data),
  get: (id: number) => http.get<Role>(`/api/v1/admin/roles/${id}`).then((res) => res.data),
  create: (data: RoleCreateRequest) => http.post<Role>("/api/v1/admin/roles", data).then((res) => res.data),
  modify: (id: number, data: RoleModifyRequest) =>
    http.put<Role>(`/api/v1/admin/roles/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`/api/v1/admin/roles/${id}`).then(() => undefined),
  // 사용자별 역할. keyword가 없으면 업무 역할이 있는 사용자와 관리자, 있으면 모든 사용자에서 찾는다
  users: (params?: RoleUserQuery & PageRequest) =>
    http.get<PageResponse<RoleUser>>("/api/v1/admin/roles/users", { params }).then((res) => res.data),
  members: (roleId: number) => http.get<RoleUser[]>(`/api/v1/admin/roles/${roleId}/members`).then((res) => res.data),
  addMember: (roleId: number, userId: number) =>
    http.post<void>(`/api/v1/admin/roles/${roleId}/members`, { userId }).then(() => undefined),
  removeMember: (roleId: number, userId: number) =>
    http.delete<void>(`/api/v1/admin/roles/${roleId}/members/${userId}`).then(() => undefined),
  // 사용자에게 준 역할. 바꿀 때는 통째로 (빈 목록이면 모두 뺀다)
  userRoles: (userId: number) => http.get<Role[]>(`/api/v1/admin/users/${userId}/roles`).then((res) => res.data),
  replaceUserRoles: (userId: number, roleIds: number[]) =>
    http.put<void>(`/api/v1/admin/users/${userId}/roles`, { roleIds }).then(() => undefined),
}
