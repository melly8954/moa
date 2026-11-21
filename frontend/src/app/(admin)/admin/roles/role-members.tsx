"use client"

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useErrorMessages } from "@/lib/error-messages"
import type { Role } from "@/types/role"

const SIZE = 20

// 역할 "구성원" (moa, specs/003-admin-roles FR-006, US5, research R13·R19)
//   고르기 목록: 검색과 서버 쪽 나눔. 쪽은 칸 안 상태로 두고 주소에 두지 않는다
//   검색어가 없으면 구성원만, 있으면 관리자 계정에서 찾고 구성원인지 표시한다 (회원은 역할을 받지 않아 나오지 않는다)
//   체크해서 "더하기"·"빼기"를 한꺼번에 한다. 다른 역할에서는 최고 관리자 계정을 체크할 수 없다 (모든 권한)
//   내가 다룰 수 없는 역할(grantable false)은 보기만 한다
//   하나라도 틀리면 서버가 아무것도 바꾸지 않는다 (회원 B001, 권한 밖 역할 A005)
//   최고 관리자 역할(fixed)은 구성원이 첫 관리자 한 명으로 고정이라 보기만 한다. 검색·체크·일괄 줄을 두지 않는다 (FR-006·FR-010, R24)
export function RoleMembers({ role }: { role: Role }) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [keyword, setKeyword] = useState("")
  const [searched, setSearched] = useState("")
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<number[]>([])
  const members = useQuery({
    queryKey: ["admin", "roles", role.id, "members", { keyword: searched, page }],
    queryFn: () => api.v1.role.members(role.id, { keyword: searched || undefined, page, size: SIZE }),
    placeholderData: keepPreviousData,
  })
  const blocked = (u: { superAdmin: boolean }) => !role.grantable || (!role.fixed && u.superAdmin)
  const selectable = (members.data?.content ?? []).filter((u) => !blocked(u)).map((u) => u.id)
  const allChecked = selectable.length > 0 && selectable.every((id) => selected.includes(id))

  const run = useMutation({
    mutationFn: ({ action }: { action: () => Promise<unknown>; done: string }) => action(),
    onSuccess: async (_, { done }) => {
      setSelected([])
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(done)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      toast.error(
        apiError.code === "A005"
          ? t(role.fixed ? "blockSuperAdminOnly" : "grantDenied")
          : apiError.code === "B001"
            ? apiError.message || describeError(apiError).message
            : describeError(apiError).message,
      )
    },
  })

  const search = (value: string) => {
    setSearched(value.trim())
    setPage(0)
    setSelected([])
  }

  if (role.fixed) {
    return (
      <div className="flex flex-col gap-2">
        <QueryState query={members} isEmpty={(d) => d.content.length === 0} empty={t("membersEmpty")}>
          {(data) => (
            <ul className="flex flex-col gap-1 rounded-lg border p-3 text-sm">
              {data.content.map((u) => (
                <li key={u.id}>
                  {u.loginId} <span className="text-muted-foreground">{u.name}</span>
                </li>
              ))}
            </ul>
          )}
        </QueryState>
        <p className="text-sm text-muted-foreground">{t("superAdminFixed")}</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <form
        role="search"
        aria-label={t("memberSearch")}
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          search(keyword)
        }}
      >
        <Input
          aria-label={t("findUser")}
          value={keyword}
          placeholder={t("findUserPlaceholder")}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Button type="submit" variant="outline">
          {t("find")}
        </Button>
      </form>
      <p className="text-sm text-muted-foreground">{t(searched ? "membersSearching" : "membersOnly")}</p>
      {selected.length > 0 && (
        <div
          role="toolbar"
          aria-label={t("memberToolbar")}
          className="flex flex-wrap items-center gap-2 rounded-lg border bg-muted px-3 py-2"
        >
          <span className="text-sm font-medium">{t("selectedPeople", { count: selected.length })}</span>
          <span className="flex-1" />
          <Button
            size="sm"
            variant="outline"
            disabled={run.isPending}
            onClick={() =>
              run.mutate({
                action: () => api.v1.role.addMembers(role.id, selected),
                done: t("membersAdded", { count: selected.length }),
              })
            }
          >
            {t("addMember")}
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={run.isPending}
            onClick={() =>
              run.mutate({
                action: () => api.v1.role.removeMembers(role.id, selected),
                done: t("membersRemoved", { count: selected.length }),
              })
            }
          >
            {t("removeMember")}
          </Button>
        </div>
      )}
      <QueryState
        query={members}
        isEmpty={(d) => d.content.length === 0}
        empty={t(searched ? "findEmpty" : "membersEmpty")}
      >
        {(data) => (
          <>
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <Checkbox
                        aria-label={t("selectAllPeople")}
                        checked={allChecked}
                        disabled={selectable.length === 0}
                        onCheckedChange={(checked) =>
                          setSelected(
                            checked === true
                              ? [...new Set([...selected, ...selectable])]
                              : selected.filter((id) => !selectable.includes(id)),
                          )
                        }
                      />
                    </TableHead>
                    <TableHead>{t("loginId")}</TableHead>
                    <TableHead>{t("name")}</TableHead>
                    <TableHead>{t("memberStatus")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((u) => (
                    <TableRow key={u.id} data-state={selected.includes(u.id) ? "selected" : undefined}>
                      <TableCell>
                        <Checkbox
                          aria-label={t("selectPerson", { loginId: u.loginId })}
                          checked={selected.includes(u.id)}
                          disabled={blocked(u)}
                          onCheckedChange={(checked) =>
                            setSelected(checked === true ? [...selected, u.id] : selected.filter((id) => id !== u.id))
                          }
                        />
                      </TableCell>
                      <TableCell>{u.loginId}</TableCell>
                      <TableCell>{u.name}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {!role.fixed && u.superAdmin ? t("allPermissions") : u.member ? t("isMember") : t("notMember")}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            {data.totalPages > 1 && (
              <nav aria-label={t("memberPages")} className="flex items-center justify-center gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  {t("prevPage")}
                </Button>
                <span className="text-sm">
                  {page + 1} / {data.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  {t("nextPage")}
                </Button>
              </nav>
            )}
          </>
        )}
      </QueryState>
    </div>
  )
}
