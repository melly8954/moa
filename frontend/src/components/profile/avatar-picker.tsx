"use client"

import { useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { api } from "@/api"
import { QueryState } from "@/components/kit/query-state"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { DEFAULT_AVATAR, isAvatarKey } from "@/lib/avatars"
import { STALE_TIME } from "@/lib/query"
import { cn } from "@/lib/utils"
import { AvatarImage } from "./avatar-image"

// moa: 기본 아바타 고르기 (specs/009-mypage FR-009, US4). 서버 목록(GET /api/v1/profile/avatars)의 그림 중 하나
//   올리기·주소 입력은 없다. 고른 값은 프로필 폼에 들어가고 "저장"할 때 함께 보낸다
export function AvatarPicker({ value, onChange }: { value: string; onChange: (avatar: string) => void }) {
  const t = useTranslations("profile")
  const [open, setOpen] = useState(false)
  const avatars = useQuery({
    queryKey: ["profile", "avatars"],
    queryFn: api.v1.profile.avatars,
    enabled: open,
    staleTime: STALE_TIME.REFERENCE,
  })
  const label = (key: string) => t(`avatarNames.${isAvatarKey(key) ? key : DEFAULT_AVATAR}`)

  return (
    <>
      <Button type="button" variant="outline" size="sm" onClick={() => setOpen(true)}>
        {t("chooseAvatar")}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("chooseAvatar")}</DialogTitle>
          </DialogHeader>
          <QueryState query={avatars}>
            {(data) => (
              <div className="grid grid-cols-4 gap-2">
                {data.avatars.map((key) => (
                  <button
                    key={key}
                    type="button"
                    aria-label={label(key)}
                    aria-pressed={key === value}
                    onClick={() => {
                      onChange(key)
                      setOpen(false)
                    }}
                    className={cn(
                      "flex items-center justify-center rounded-lg p-1.5 outline-none hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50",
                      key === value && "ring-2 ring-primary",
                    )}
                  >
                    <AvatarImage avatar={key} className="size-14" />
                  </button>
                ))}
              </div>
            )}
          </QueryState>
        </DialogContent>
      </Dialog>
    </>
  )
}
