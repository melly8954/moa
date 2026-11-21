"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldContent, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Skeleton } from "@/components/ui/skeleton"
import { useErrorMessages } from "@/lib/error-messages"
import { useHasPermission } from "@/lib/me"
import type { Role } from "@/types/role"
import type { AdminUser } from "@/types/user-admin"

// 사용자 드로어의 역할 칸 (moa, specs/002-admin-users FR-005·FR-007, research R11, 2026-11-21)
//   관리자 계정만 역할을 받는다. 회원이면 계정 종류를 관리자로 바꾸라고 안내한다
//   권한 관리 권한(ROLE_MANAGE)이 있으면 체크해서 주고 뺀다. 저장은 권한 관리의 API(PUT /users/{id}/roles)를 그대로 쓴다
//   체크할 수 없는 역할(최고 관리자만, 내가 갖지 않은 권한)은 꺼진 채 이유를 보인다. 서버도 같은 규칙으로 막는다
//   최고 관리자 역할을 고르면 그 역할만 남는다 (모든 권한이라 다른 역할은 뜻이 없다, specs/003-admin-roles FR-010)
//   권한 관리 권한이 없으면 받은 역할 이름만 보인다
export function UserRoles({ user }: { user: AdminUser }) {
  const t = useTranslations("userAdmin")
  const canManage = useHasPermission("ROLE_MANAGE")

  if (user.role !== "ADMIN") {
    return <p className="text-sm text-muted-foreground">{t("memberNoRoles")}</p>
  }
  if (!canManage) {
    return (
      <div className="flex flex-col gap-1 text-sm">
        <span>{rolesText(user, t)}</span>
        <span className="text-muted-foreground">{t("rolesReadOnly")}</span>
      </div>
    )
  }
  return <RoleChecks user={user} />
}

function RoleChecks({ user }: { user: AdminUser }) {
  const t = useTranslations("userAdmin")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const given = useQuery({
    queryKey: ["admin", "users", user.id, "roles"],
    queryFn: () => api.v1.role.userRoles(user.id),
  })
  const givenIds = (given.data ?? []).map((r) => r.id)
  // 처음 값은 받은 역할. 받은 역할이 바뀌면(저장, 다른 곳에서 바꿈) 다시 맞춘다
  const [checked, setChecked] = useState<number[]>(givenIds)
  const [lastGiven, setLastGiven] = useState(given.data)
  if (given.data !== lastGiven) {
    setLastGiven(given.data)
    setChecked(givenIds)
  }
  const superRole = roles.data?.find((r) => r.fixed)
  const superChecked = !!superRole && checked.includes(superRole.id)
  const dirty = checked.length !== givenIds.length || checked.some((id) => !givenIds.includes(id))

  const save = useMutation({
    mutationFn: () => api.v1.role.replaceUserRoles(user.id, checked),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "users"] })
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t("rolesSaved"))
    },
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(
        apiError.code === "B001"
          ? apiError.message || describeError(apiError).message
          : describeError(apiError).message,
      )
    },
  })

  const toggle = (role: Role, next: boolean) => {
    if (role.fixed) setChecked(next ? [role.id] : checked.filter((id) => id !== role.id))
    else setChecked(next ? [...checked, role.id] : checked.filter((id) => id !== role.id))
  }

  if (!roles.data || !given.data) {
    return <Skeleton className="h-24 w-full" />
  }
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-2">
        {roles.data.map((role) => {
          const reason = !role.grantable
            ? t(role.blockReason === "SUPER_ADMIN_ONLY" ? "roleSuperAdminOnly" : "roleWider")
            : superChecked && !role.fixed
              ? t("roleWithSuperAdmin")
              : undefined
          const id = `user-role-${role.id}`
          return (
            <Field key={role.id} orientation="horizontal">
              <Checkbox
                id={id}
                checked={checked.includes(role.id)}
                disabled={!!reason || save.isPending}
                onCheckedChange={(next) => toggle(role, next === true)}
              />
              <FieldContent>
                <FieldLabel htmlFor={id} className="font-normal">
                  {role.name}
                </FieldLabel>
                <FieldDescription className="text-xs">
                  {role.fixed ? t("allPermissions") : role.permissionNames.join(", ") || t("noMenus")}
                  {reason && ` · ${reason}`}
                </FieldDescription>
              </FieldContent>
            </Field>
          )
        })}
      </div>
      <div className="flex items-center gap-2">
        <Button size="sm" variant="outline" disabled={!dirty || save.isPending} onClick={() => save.mutate()}>
          {t("saveRoles")}
        </Button>
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
    </div>
  )
}

// 목록 열·보기 전용 칸의 역할 글자 (specs/002-admin-users FR-006)
export function rolesText(user: AdminUser, t: ReturnType<typeof useTranslations<"userAdmin">>) {
  if (user.role !== "ADMIN") return ""
  if (user.superAdmin) return t("superAdmin")
  return user.accessRoles.length > 0 ? user.accessRoles.map((r) => r.name).join(", ") : t("noAccessRoles")
}
