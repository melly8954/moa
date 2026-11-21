"use client"

import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { type Column, DataTable } from "@/components/kit/data-table"
import { ListPage } from "@/components/kit/page-header"
import { ChoiceFilter, KeywordFilter, SearchField, SearchPanel, SearchRow } from "@/components/kit/search-panel"
import { Button } from "@/components/ui/button"
import { useListSearch } from "@/lib/list-search"
import { useMe } from "@/lib/me"
import type { AdminUser } from "@/types/user-admin"
import { CreateUserDrawer, UserDrawer, UserStatus } from "./user-drawer"
import { UserExtraDialogs, useUserExtraActions } from "./user-extras"
import { rolesText } from "./user-roles"
import {
  RenameDialog,
  RoleChangeDialog,
  ROLES,
  TemporaryPasswordDialog,
  useInvalidateUsers,
  useUserErrorText,
} from "./user-dialogs"

// 사용자 관리 (user-admin 헬퍼, 권한 USER_MANAGE)
// moa: 관리 화면 정리 (specs/002-admin-users FR-005~FR-008, research R6·R7·R11)
//   목록에 역할 열(최고 관리자·역할 없음·역할 이름들, 회원은 빈 칸). 아이디를 누르면 드로어에서 다룬다
//   만들기도 드로어에서 하고 임시 비밀번호를 드로어 안에 한 번 보인다. 관리자 계정의 역할도 드로어에서 준다
// 막는 것 (서버도 막는다. 화면은 버튼을 끈다)
//   - 내 행: 계정 종류 바꾸기·잠그기·삭제 (B001)
//   - 다룰 수 없는 계정(protection): 최고 관리자 계정, 나보다 권한이 많은 관리자 계정 (A005)
//   - 비밀번호 없는 계정(소셜): 비밀번호 초기화 (B001)
const DEFAULTS = { keyword: "", role: "", locked: "" }

export default function UsersPage() {
  const t = useTranslations("userAdmin")
  const me = useMe().data
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
  const [openId, setOpenId] = useState<number>()
  const [renaming, setRenaming] = useState<AdminUser>()
  const [changingRole, setChangingRole] = useState<AdminUser>()
  const [password, setPassword] = useState<{ loginId: string; value: string }>()

  const run = useMutation({
    mutationFn: (action: () => Promise<unknown>) => action(),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(errorText(error)),
  })

  const isSelf = (u: AdminUser) => u.id === me?.id

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
      setOpenId(undefined)
      toast.success(t("removed"))
    })
  }

  const columns: Column<AdminUser>[] = [
    {
      key: "loginId",
      header: t("loginId"),
      cell: (u) => (
        <button type="button" className="text-left underline-offset-4 hover:underline" onClick={() => setOpenId(u.id)}>
          {u.loginId}
        </button>
      ),
    },
    { key: "name", header: t("name") },
    { key: "role", header: t("role"), kind: "status", cell: (u) => t(`roles.${u.role}`) },
    {
      key: "accessRoles",
      header: t("accessRoles"),
      cell: (u) => (
        <span className={u.superAdmin || u.accessRoles.length > 0 ? undefined : "text-muted-foreground"}>
          {rolesText(u, t)}
        </span>
      ),
    },
    { key: "status", header: t("status"), kind: "status", cell: (u) => <UserStatus user={u} /> },
    { key: "createdAt", header: t("createdAt"), kind: "datetime" },
  ]
  // 드로어는 목록의 최신 값을 본다 (바꾸면 목록을 다시 읽는다)
  const opened = users.data?.content.find((u) => u.id === openId)

  return (
    <ListPage
      title={t("title")}
      description={t.rich("description", { link: (chunks) => <Link href="/admin/roles">{chunks}</Link> })}
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
      <CreateUserDrawer open={creating} onOpenChange={setCreating} />
      <UserDrawer
        user={opened}
        onClose={() => setOpenId(undefined)}
        isSelf={!!opened && isSelf(opened)}
        extraActions={extraActions}
        onRename={() => opened && setRenaming(opened)}
        onChangeRole={() => opened && setChangingRole(opened)}
        onToggleLock={() => opened && void toggleLock(opened)}
        onResetPassword={() => opened && void resetPassword(opened)}
        onRemove={() => opened && void remove(opened)}
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
