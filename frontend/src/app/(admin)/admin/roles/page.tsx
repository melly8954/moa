"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useErrorMessages } from "@/lib/error-messages"
import { formatNumber } from "@/lib/format"
import type { Role } from "@/types/role"
import { MembersDialog } from "./members-dialog"
import { RoleDialog } from "./role-dialog"
import { UserRolesTab } from "./user-roles-tab"

// 역할 관리 (rbac 헬퍼, 권한 ROLE_MANAGE). 역할에 권한을 묶고, 사용자에게 역할을 주면 그 권한을 갖는다
// moa: 사용자에게 역할을 주는 것은 이 화면에서만 한다. "사용자별 역할" 탭과 역할의 "구성원" (specs/003-admin-roles FR-005~FR-007)
export default function RolesPage() {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: api.v1.role.list })
  const [editing, setEditing] = useState<{ role?: Role } | null>(null)
  const [membersOf, setMembersOf] = useState<Role>()
  const [tab, setTab] = useState<"roles" | "users">("roles")

  const remove = useMutation({
    mutationFn: (id: number) => api.v1.role.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t("removed"))
    },
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  const askRemove = async (role: Role) => {
    const ok = await confirm({
      title: t("removeTitle", { name: role.name }),
      description: t("removeDescription", { count: role.userCount }),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate(role.id)
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("pageDescription")}
        actions={
          tab === "roles" && (
            <Button onClick={() => setEditing({})}>
              <Plus />
              {t("add")}
            </Button>
          )
        }
      />
      <Tabs value={tab} onValueChange={(value) => setTab(value as "roles" | "users")}>
        <TabsList aria-label={t("tabs")}>
          <TabsTrigger value="roles">{t("tabRoles")}</TabsTrigger>
          <TabsTrigger value="users">{t("tabUsers")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "users" ? (
        <UserRolesTab />
      ) : (
        <QueryState query={roles} isEmpty={(d) => d.length === 0} empty={t("empty")}>
          {(list) => (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("code")}</TableHead>
                    <TableHead>{t("name")}</TableHead>
                    <TableHead>{t("description")}</TableHead>
                    <TableHead className="w-(--col-number) text-right">{t("permissionCount")}</TableHead>
                    <TableHead className="w-(--col-number) text-right">{t("userCount")}</TableHead>
                    <TableHead className="w-(--col-actions) text-right">
                      <span className="sr-only">{t("actions")}</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {list.map((role) => (
                    <TableRow key={role.id}>
                      <TableCell className="font-mono">{role.code}</TableCell>
                      <TableCell>{role.name}</TableCell>
                      <TableCell>
                        <div className="max-w-(--col-text-max) truncate text-muted-foreground">{role.description}</div>
                      </TableCell>
                      <TableCell className="text-right">{formatNumber(role.permissions.length)}</TableCell>
                      <TableCell className="text-right">{formatNumber(role.userCount)}</TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={t("membersOf", { name: role.name })}
                          onClick={() => setMembersOf(role)}
                        >
                          {t("members")}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={t("editOf", { name: role.name })}
                          onClick={() => setEditing({ role })}
                        >
                          {t("editShort")}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={t("removeOf", { name: role.name })}
                          onClick={() => void askRemove(role)}
                        >
                          {t("remove")}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </QueryState>
      )}
      <MembersDialog role={membersOf} onOpenChange={(open) => !open && setMembersOf(undefined)} />
      <RoleDialog role={editing?.role} open={editing !== null} onOpenChange={(open) => !open && setEditing(null)} />
    </>
  )
}
