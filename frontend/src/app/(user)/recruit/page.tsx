"use client"

import { useInfiniteQuery, useQuery } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useEffect, useRef, useState } from "react"
import { api } from "@/api"
import { CodeSelect } from "@/components/code/code-select"
import { LinkButton } from "@/components/kit/link-button"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { PartyCard } from "@/components/party/party-card"
import { Button } from "@/components/ui/button"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader } from "@/components/ui/empty"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { signInPath } from "@/lib/next-path"
import { modeGroupOf } from "@/lib/schemas/party"
import { useAuthStore } from "@/stores/auth"
import { CreatePartyDialog } from "./create-party-dialog"

const SIZE = 20
const PATH = "/recruit"

// 모집 페이지 (specs/008-party US1·US2, 화면 "모집 페이지", T019). 로그인 없이 연다
//   위쪽: 파티 만들기(이미 파티에 속하면 "내 파티로 가기"), 초대 코드 입력, 게임·모드 거르기
//   카드는 최신순이고, 목록 끝에 닿거나 "더 보기"를 누르면 다음 쪽을 불러온다
//   로그인하지 않은 사람이 만들기·초대 코드를 누르면 로그인한 뒤 돌아온다
export default function RecruitPage() {
  const t = useTranslations("party.recruit")
  const router = useRouter()
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const [gameCode, setGameCode] = useState("")
  const [modeCode, setModeCode] = useState("")
  const [inviteCode, setInviteCode] = useState("")
  const [creating, setCreating] = useState(false)

  const parties = useInfiniteQuery({
    queryKey: ["parties", "list", { gameCode, modeCode }],
    queryFn: ({ pageParam }) =>
      api.v1.party.getPage({
        gameCode: gameCode || undefined,
        modeCode: modeCode || undefined,
        page: pageParam,
        size: SIZE,
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
  const myParty = useQuery({ queryKey: ["parties", "me"], queryFn: api.v1.party.me, enabled: signedIn })
  const inParty = !!myParty.data?.party

  // 목록 끝이 보이면 다음 쪽
  const sentinel = useRef<HTMLDivElement>(null)
  const { hasNextPage, isFetchingNextPage, fetchNextPage } = parties
  useEffect(() => {
    const el = sentinel.current
    if (!el || !hasNextPage) return
    const observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && !isFetchingNextPage) void fetchNextPage()
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

  const openCreate = () => (signedIn ? setCreating(true) : router.push(signInPath(PATH)))
  const createButton = inParty ? (
    <LinkButton href="/my-party">{t("goToMyParty")}</LinkButton>
  ) : (
    <Button onClick={openCreate}>
      <Plus />
      {t("create")}
    </Button>
  )

  const submitInvite = (e: React.FormEvent) => {
    e.preventDefault()
    const code = inviteCode.trim().toUpperCase()
    if (code) router.push(`/invite/${encodeURIComponent(code)}`)
  }

  return (
    <div className="flex flex-col gap-(--page-gap)">
      <PageHeader title={t("title")} description={t("description")} actions={createButton} />

      <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
        <form onSubmit={submitInvite} className="flex items-end gap-2">
          <Field>
            <FieldLabel htmlFor="recruit-invite-code">{t("inviteCode")}</FieldLabel>
            <Input
              id="recruit-invite-code"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
              placeholder={t("inviteCodePlaceholder")}
              autoComplete="off"
              autoCapitalize="characters"
              spellCheck={false}
              maxLength={20}
              className="font-mono"
            />
          </Field>
          <Button type="submit" variant="outline" disabled={!inviteCode.trim()}>
            {t("enterInvite")}
          </Button>
        </form>
        <Field className="sm:w-48">
          <FieldLabel htmlFor="recruit-game">{t("game")}</FieldLabel>
          <CodeSelect
            groupCode="GAME"
            id="recruit-game"
            value={gameCode}
            onChange={(value) => {
              setGameCode(value)
              setModeCode("")
            }}
            placeholder={t("allGames")}
          />
        </Field>
        <Field className="sm:w-48">
          <FieldLabel htmlFor="recruit-mode">{t("mode")}</FieldLabel>
          {gameCode ? (
            <CodeSelect
              groupCode={modeGroupOf(gameCode)}
              id="recruit-mode"
              value={modeCode}
              onChange={setModeCode}
              placeholder={t("allModes")}
            />
          ) : (
            // 모드는 게임을 고른 뒤에만 고른다
            <NativeSelect id="recruit-mode" value="" disabled>
              <NativeSelectOption value="">{t("chooseGameFirst")}</NativeSelectOption>
            </NativeSelect>
          )}
        </Field>
      </div>

      <QueryState query={parties}>
        {(data) => {
          const items = data.pages.flatMap((p) => p.content)
          if (items.length === 0)
            return (
              <Empty>
                <EmptyHeader>
                  <EmptyDescription>{t(gameCode ? "emptyFiltered" : "empty")}</EmptyDescription>
                </EmptyHeader>
                {!inParty && (
                  <EmptyContent>
                    <Button onClick={openCreate}>
                      <Plus />
                      {t("create")}
                    </Button>
                  </EmptyContent>
                )}
              </Empty>
            )
          return (
            <>
              <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {items.map((party) => (
                  <li key={party.id}>
                    <PartyCard party={party} />
                  </li>
                ))}
              </ul>
              <div ref={sentinel} />
              {hasNextPage && (
                <div className="flex justify-center">
                  <Button variant="outline" disabled={isFetchingNextPage} onClick={() => void fetchNextPage()}>
                    {t("more")}
                  </Button>
                </div>
              )}
            </>
          )
        }}
      </QueryState>

      {creating && <CreatePartyDialog open={creating} onOpenChange={setCreating} />}
    </div>
  )
}
