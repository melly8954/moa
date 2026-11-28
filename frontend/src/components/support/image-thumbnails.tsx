"use client"

import { X } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"

// 문의 첨부 이미지 줄 (specs/017-customer-center FR-007, contracts/screens.md)
//   작은 그림을 누르면 창에서 크게 본다. onRemove를 주면(문의 쓰기) 그림마다 빼기 단추가 붙는다
//   관리 드로어, 회원 상세, 쓰기 화면이 같이 쓴다
export function ImageThumbnails({ urls, onRemove }: { urls: string[]; onRemove?: (index: number) => void }) {
  const t = useTranslations("support")
  const [opened, setOpened] = useState<number>()
  if (urls.length === 0) return null
  const openedUrl = opened === undefined ? undefined : urls[opened]
  return (
    <>
      <ul className="flex flex-wrap gap-2" aria-label={t("attachments")}>
        {urls.map((url, index) => (
          <li key={url} className="relative">
            <button
              type="button"
              className="block overflow-hidden rounded-md border bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              aria-label={t("imageOpen", { number: index + 1 })}
              onClick={() => setOpened(index)}
            >
              {/* eslint-disable-next-line @next/next/no-img-element -- 업로드 이미지 주소를 그대로 보인다 (rich-text 보기와 같다) */}
              <img src={url} alt="" className="size-20 object-cover" />
            </button>
            {onRemove && (
              <Button
                type="button"
                variant="secondary"
                size="icon-xs"
                className="absolute -top-2 -right-2 rounded-full"
                aria-label={t("imageRemove", { number: index + 1 })}
                onClick={() => onRemove(index)}
              >
                <X />
              </Button>
            )}
          </li>
        ))}
      </ul>
      <Dialog open={openedUrl !== undefined} onOpenChange={(open) => !open && setOpened(undefined)}>
        <DialogContent className="sm:max-w-3xl">
          <DialogTitle>{t("imageTitle", { number: (opened ?? 0) + 1 })}</DialogTitle>
          {openedUrl && (
            // eslint-disable-next-line @next/next/no-img-element -- 업로드 이미지 주소를 그대로 보인다
            <img src={openedUrl} alt="" className="max-h-[75vh] w-full object-contain" />
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}
