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

// 역할 드로어 "구성원" 탭 (moa, specs/003-admin-roles FR-006, US4, research R13)
//   고르기 목록: 검색과 서버 쪽 나눔. 쪽은 드로어 안 상태로 두고 주소에 두지 않는다
//   검색어가 없으면 구성원만, 있으면 모든 사용자에서 찾고 구성원인지 표시한다
//   체크해서 "더하기"·"빼기"를 한꺼번에 한다. 관리자(ADMIN)는 모든 권한을 가져 체크할 수 없다
//   하나라도 틀리면 서버가 아무것도 바꾸지 않는다 (ADMIN B001, 권한 밖 역할 A005)
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
  const selectable = (members.data?.content ?? []).filter((u) => u.role !== "ADMIN").map((u) => u.id)
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
          ? t("grantDenied")
          : apiError.code === "B001"
            ? t("adminNotAllowed")
            : describeError(apiError).message,
      )
    },
  })

  const search = (value: string) => {
    setSearched(value.trim())
    setPage(0)
    setSelected([])
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
                          disabled={u.role === "ADMIN"}
                          onCheckedChange={(checked) =>
                            setSelected(checked === true ? [...selected, u.id] : selected.filter((id) => id !== u.id))
                          }
                        />
                      </TableCell>
                      <TableCell>{u.loginId}</TableCell>
                      <TableCell>{u.name}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {u.role === "ADMIN" ? t("allPermissions") : u.member ? t("isMember") : t("notMember")}
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
