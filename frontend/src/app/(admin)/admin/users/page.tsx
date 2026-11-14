"use client"

import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query"
import { MoreHorizontal, Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import { ChoiceFilter, KeywordFilter, SearchField, SearchPanel, SearchRow } from "@/components/kit/search-panel"
import { StatusText } from "@/components/kit/status-text"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useListSearch } from "@/lib/list-search"
import { useMe } from "@/lib/me"
import type { AdminUser } from "@/types/user-admin"
import { UserExtraDialogs, useUserExtraActions } from "./user-extras"
import {
  CreateUserDialog,
  RenameDialog,
  RoleChangeDialog,
  ROLES,
  TemporaryPasswordDialog,
  useInvalidateUsers,
  useUserErrorText,
} from "./user-dialogs"

// 사용자 관리 (user-admin 헬퍼, 권한 USER_MANAGE)
// 막는 것 (서버도 막는다. 화면은 버튼을 끈다)
//   - 내 행: 역할 바꾸기·잠그기·삭제 (B001)
//   - 내가 ADMIN이 아니면 ADMIN 행 전체, ADMIN 역할 주기 (A005)
//   - 비밀번호 없는 계정(소셜): 비밀번호 초기화 (B001)
const DEFAULTS = { keyword: "", role: "", locked: "" }

export default function UsersPage() {
  const t = useTranslations("userAdmin")
  const me = useMe().data
  const iAmAdmin = me?.role === "ADMIN"
  const search = useListSearch({ defaults: DEFAULTS })
  const users = useQuery({
    queryKey: ["admin", "users", search.params],
    queryFn: () => api.v1.userAdmin.getPage(search.params),
    placeholderData: keepPreviousData,
  })
  const invalidate = useInvalidateUsers()
  const errorText = useUserErrorText()
  const extraActions = useUserExtraActions()
  const [creating, setCreating] = useState(false)
  const [renaming, setRenaming] = useState<AdminUser>()
  const [changingRole, setChangingRole] = useState<AdminUser>()
  const [password, setPassword] = useState<{ loginId: string; value: string }>()

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(errorText(error)),
  })

  const isSelf = (u: AdminUser) => u.id === me?.id
  // 내가 ADMIN이 아니면 ADMIN 계정은 손대지 못한다
  const protectedRow = (u: AdminUser) => !iAmAdmin && u.role === "ADMIN"

  const toggleLock = async (u: AdminUser) => {
    const ok = await confirm({
      title: t(u.locked ? "unlockTitle" : "lockTitle", { loginId: u.loginId }),
      description: u.locked ? undefined : t("lockDescription"),
      destructive: !u.locked,
    })
    if (!ok) return
    run.mutate(async () => {
      await (u.locked ? api.v1.userAdmin.unlock(u.id) : api.v1.userAdmin.lock(u.id))
      toast.success(t(u.locked ? "unlocked" : "locked"))
    })
  }

  const resetPassword = async (u: AdminUser) => {
    if (!(await confirm({ title: t("resetTitle", { loginId: u.loginId }), description: t("resetDescription") }))) return
    run.mutate(async () => {
      const { temporaryPassword } = await api.v1.userAdmin.resetPassword(u.id)
      setPassword({ loginId: u.loginId, value: temporaryPassword })
    })
  }

  const remove = async (u: AdminUser) => {
    const ok = await confirm({
      title: t("removeTitle", { loginId: u.loginId }),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    run.mutate(async () => {
      await api.v1.userAdmin.remove(u.id)
      toast.success(t("removed"))
    })
  }

  const columns: Column<AdminUser>[] = [
    { key: "loginId", header: t("loginId") },
    { key: "name", header: t("name") },
    { key: "role", header: t("role"), kind: "status", cell: (u) => t(`roles.${u.role}`) },
    {
      key: "status",
      header: t("status"),
      kind: "status",
      cell: (u) =>
        u.locked ? (
          <StatusText tone="negative">{t("statusLocked")}</StatusText>
        ) : u.passwordChangeRequired ? (
          <StatusText tone="neutral">{t("statusPasswordPending")}</StatusText>
        ) : (
          <StatusText tone="positive">{t("statusActive")}</StatusText>
        ),
    },
    { key: "createdAt", header: t("createdAt"), kind: "datetime" },
    {
      key: "actions",
      header: t("actions"),
      kind: "actions",
      cell: (u) => (
        <DropdownMenu>
          <DropdownMenuTrigger
            render={<Button variant="ghost" size="icon-sm" aria-label={t("actionsOf", { loginId: u.loginId })} />}
          >
            <MoreHorizontal />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem disabled={protectedRow(u)} onClick={() => setRenaming(u)}>
              {t("rename")}
            </DropdownMenuItem>
            <DropdownMenuItem disabled={protectedRow(u) || isSelf(u) || !iAmAdmin} onClick={() => setChangingRole(u)}>
              {t("changeRole")}
            </DropdownMenuItem>
            <DropdownMenuItem disabled={protectedRow(u) || isSelf(u)} onClick={() => void toggleLock(u)}>
              {t(u.locked ? "unlock" : "lock")}
            </DropdownMenuItem>
            <DropdownMenuItem disabled={protectedRow(u) || !u.hasPassword} onClick={() => void resetPassword(u)}>
              {t("resetPassword")}
            </DropdownMenuItem>
            {extraActions.map((action) => (
              <DropdownMenuItem
                key={action.key}
                disabled={protectedRow(u) || action.disabled?.(u)}
                onClick={() => action.onSelect(u)}
              >
                {action.label}
              </DropdownMenuItem>
            ))}
            <DropdownMenuSeparator />
            <DropdownMenuItem
              variant="destructive"
              disabled={protectedRow(u) || isSelf(u)}
              onClick={() => void remove(u)}
            >
              {t("remove")}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ]

  return (
    <ListPage
      title={t("title")}
      description={t("description")}
      actions={
        <Button onClick={() => setCreating(true)}>
          <Plus />
          {t("create")}
        </Button>
      }
      search={
        <SearchPanel search={search}>
          <SearchRow>
            <SearchField label={t("role")}>
              <ChoiceFilter name="role" options={ROLES.map((r) => ({ value: r, label: t(`roles.${r}`) }))} />
            </SearchField>
            <SearchField label={t("status")}>
              <ChoiceFilter
                name="locked"
                options={[
                  { value: "false", label: t("statusActive") },
                  { value: "true", label: t("statusLocked") },
                ]}
              />
            </SearchField>
          </SearchRow>
          <SearchField label={t("keyword")}>
            <KeywordFilter name="keyword" placeholder={t("keywordPlaceholder")} />
          </SearchField>
        </SearchPanel>
      }
    >
      <DataTable caption={t("title")} columns={columns} query={users} search={search} rowKey={(u) => u.id} />
      <CreateUserDialog
        open={creating}
        onOpenChange={setCreating}
        canGrantAdmin={iAmAdmin}
        onCreated={(loginId, value) => setPassword({ loginId, value })}
      />
      <RenameDialog user={renaming} onClose={() => setRenaming(undefined)} />
      <RoleChangeDialog user={changingRole} onClose={() => setChangingRole(undefined)} />
      <TemporaryPasswordDialog
        value={password?.value}
        loginId={password?.loginId}
        onClose={() => setPassword(undefined)}
      />
      <UserExtraDialogs />
    </ListPage>
  )
}
