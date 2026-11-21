"use client"

import { useQuery } from "@tanstack/react-query"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldContent, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Skeleton } from "@/components/ui/skeleton"
import { useHasPermission } from "@/lib/me"
import type { Role as AccountType } from "@/types/user"
import type { AdminUser } from "@/types/user-admin"

// 사용자 드로어의 역할 칸 (moa, specs/002-admin-users FR-005·FR-007, research R11·R16)
//   드로어 한 폼의 일부다. 체크한 값(value)은 폼이 갖고, 저장도 드로어 아래 저장 단추가 한다 (2026-11-22. 예전 "역할 저장" 단추)
//   accountType은 폼에서 고른 계정 종류다. 관리자 계정만 역할을 받는다. 회원을 관리자로 고르면 체크 칸이 바로 나타난다
//   권한 관리 권한(ROLE_MANAGE)이 있으면 체크해서 주고 뺀다. 저장 API는 권한 관리의 PUT /users/{id}/roles 그대로다
//   체크할 수 없는 역할(사용자 관리 권한이 든 역할은 최고 관리자만, 내가 갖지 않은 권한)은 꺼진 채 이유를 보인다
//   최고 관리자 역할은 누구도 줄 수 없으므로 목록에 두지 않는다 (2026-11-22, specs/003-admin-roles FR-010)
//   최고 관리자 계정은 체크 목록 대신 "최고 관리자 (모든 권한)"만 보인다. 권한 관리 권한이 없으면 받은 역할 이름만 보인다
export function UserRoles({
  user,
  accountType,
  value,
  onChange,
  disabled,
}: {
  user: AdminUser
  accountType: AccountType
  value: number[]
  onChange: (value: number[]) => void
  disabled?: boolean
}) {
  const t = useTranslations("userAdmin")
  const canManage = useHasPermission("ROLE_MANAGE")

  if (accountType !== "ADMIN") {
    // 관리자 계정을 회원으로 고르면 빠질 역할을 알린다 (FR-005, US4-AC5)
    if (user.role === "ADMIN" && (user.superAdmin || user.accessRoles.length > 0)) {
      return (
        <p className="text-sm text-muted-foreground">
          {t("demoteWarning", {
            roles: user.superAdmin ? t("superAdmin") : user.accessRoles.map((r) => r.name).join(", "),
          })}
        </p>
      )
    }
    return <p className="text-sm text-muted-foreground">{t("memberNoRoles")}</p>
  }
  if (user.superAdmin) {
    return <p className="text-sm">{t("superAdminRoles")}</p>
  }
  if (!canManage) {
    return (
      <div className="flex flex-col gap-1 text-sm">
        {user.role === "ADMIN" && <span>{rolesText(user, t)}</span>}
        <span className="text-muted-foreground">{t("rolesReadOnly")}</span>
      </div>
    )
  }
  return <RoleChecks value={value} onChange={onChange} disabled={disabled} />
}

// 이 계정이 받은 역할 (폼의 처음 값). 권한 관리 권한이 있고, 역할을 체크로 다루는 관리자 계정일 때만 읽는다
export function useGivenRoles(user: AdminUser | undefined) {
  const canManage = useHasPermission("ROLE_MANAGE")
  const enabled = !!user && user.role === "ADMIN" && !user.superAdmin && canManage
  const given = useQuery({
    queryKey: ["admin", "users", user?.id, "roles"],
    queryFn: () => api.v1.role.userRoles(user!.id),
    enabled,
  })
  return { enabled, canManage, isPending: enabled && given.isPending, ids: (given.data ?? []).map((r) => r.id) }
}

function RoleChecks({
  value,
  onChange,
  disabled,
}: {
  value: number[]
  onChange: (value: number[]) => void
  disabled?: boolean
}) {
  const t = useTranslations("userAdmin")
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })

  if (!roles.data) {
    return <Skeleton className="h-24 w-full" />
  }
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-2">
        {roles.data
          .filter((role) => !role.fixed)
          .map((role) => {
            const reason = role.grantable
              ? undefined
              : t(role.blockReason === "SUPER_ADMIN_ONLY" ? "roleSuperAdminOnly" : "roleWider")
            const id = `user-role-${role.id}`
            return (
              <Field key={role.id} orientation="horizontal">
                <Checkbox
                  id={id}
                  checked={value.includes(role.id)}
                  disabled={!!reason || disabled}
                  onCheckedChange={(next) =>
                    onChange(next === true ? [...value, role.id] : value.filter((v) => v !== role.id))
                  }
                />
                <FieldContent>
                  <FieldLabel htmlFor={id} className="font-normal">
                    {role.name}
                  </FieldLabel>
                  <FieldDescription className="text-xs">
                    {role.permissionNames.join(", ") || t("noMenus")}
                    {reason && ` · ${reason}`}
                  </FieldDescription>
                </FieldContent>
              </Field>
            )
          })}
      </div>
      <span className="text-xs text-muted-foreground">
        {t.rich("rolesHint", {
          link: (chunks) => (
            <Link href="/admin/roles" className="text-foreground underline underline-offset-4">
              {chunks}
            </Link>
          ),
        })}
      </span>
    </div>
  )
}

// 목록 열·보기 전용 칸의 역할 글자 (specs/002-admin-users FR-006)
export function rolesText(user: AdminUser, t: ReturnType<typeof useTranslations<"userAdmin">>) {
  if (user.role !== "ADMIN") return ""
  if (user.superAdmin) return t("superAdmin")
  return user.accessRoles.length > 0 ? user.accessRoles.map((r) => r.name).join(", ") : t("noAccessRoles")
}
