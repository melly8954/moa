import type { PageRequest, PageResponse } from "@/types/api"
import type {
  Permission,
  PermissionTree,
  Role,
  RoleCreateRequest,
  RoleModifyRequest,
  RoleMember,
  RoleMemberQuery,
} from "@/types/role"
import { http } from "./client"

// 역할·권한 (rbac 헬퍼, 권한 ROLE_MANAGE). api.v1.role로 부른다
//   내가 가진 권한 밖을 다루면 서버가 A005로 막는다 (권한 상승 방지)
// moa: 권한 트리·구성원·일괄 지우기 (specs/003-admin-roles FR-004~FR-006). 사용자 관리 권한 없이 권한 관리 권한으로 쓴다
//   최고 관리자 역할은 바꾸거나 지울 수 없고(B001), 회원에게는 역할을 줄 수 없다(B001) (FR-002·FR-010)
export const roleApi = {
  permissions: () => http.get<Permission[]>("/api/v1/admin/permissions").then((res) => res.data),
  permissionTree: () => http.get<PermissionTree>("/api/v1/admin/roles/permission-tree").then((res) => res.data),
  list: () => http.get<Role[]>("/api/v1/admin/roles").then((res) => res.data),
  get: (id: number) => http.get<Role>(`/api/v1/admin/roles/${id}`).then((res) => res.data),
  create: (data: RoleCreateRequest) => http.post<Role>("/api/v1/admin/roles", data).then((res) => res.data),
  modify: (id: number, data: RoleModifyRequest) =>
    http.put<Role>(`/api/v1/admin/roles/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`/api/v1/admin/roles/${id}`).then(() => undefined),
  // moa: 여러 역할을 한꺼번에 지운다. 하나라도 틀리면 아무것도 지우지 않는다 (specs/003-admin-roles FR-005)
  bulkDelete: (ids: number[]) => http.post<void>("/api/v1/admin/roles/bulk-delete", { ids }).then(() => undefined),
  // moa: 구성원 (FR-006). keyword가 없으면 구성원만, 있으면 관리자 계정에서 찾고 member로 구성원인지 알린다
  members: (roleId: number, params?: RoleMemberQuery & PageRequest) =>
    http.get<PageResponse<RoleMember>>(`/api/v1/admin/roles/${roleId}/members`, { params }).then((res) => res.data),
  addMembers: (roleId: number, userIds: number[]) =>
    http.post<void>(`/api/v1/admin/roles/${roleId}/members`, { userIds }).then(() => undefined),
  removeMembers: (roleId: number, userIds: number[]) =>
    http.post<void>(`/api/v1/admin/roles/${roleId}/members/remove`, { userIds }).then(() => undefined),
  // 사용자에게 준 역할. 바꿀 때는 통째로 (빈 목록이면 모두 뺀다). moa: 사용자 관리 드로어도 쓴다 (`002-admin-users` FR-005)
  userRoles: (userId: number) => http.get<Role[]>(`/api/v1/admin/users/${userId}/roles`).then((res) => res.data),
  replaceUserRoles: (userId: number, roleIds: number[]) =>
    http.put<void>(`/api/v1/admin/users/${userId}/roles`, { roleIds }).then(() => undefined),
}
