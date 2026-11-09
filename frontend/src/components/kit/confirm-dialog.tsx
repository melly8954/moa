"use client"

import { useTranslations } from "next-intl"
import { create } from "zustand"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

export type ConfirmOptions = {
  title: string
  description?: string
  // 기본은 "확인"·"취소" (common.confirm, common.cancel)
  confirmLabel?: string
  cancelLabel?: string
  // 지우기처럼 되돌릴 수 없는 일이면 true (빨간 버튼)
  destructive?: boolean
}

type Pending = ConfirmOptions & { resolve: (ok: boolean) => void }

const useConfirmStore = create<{ pending?: Pending }>(() => ({}))

// 확인 창을 띄우고 답을 기다린다. 확인이면 true, 취소·바깥 누름·Esc면 false
//   if (!(await confirm({ title: t("deleteTitle"), destructive: true }))) return
// 창은 하나만 뜬다. 앞 창이 열려 있으면 앞 창을 취소로 닫는다
export function confirm(options: ConfirmOptions): Promise<boolean> {
  useConfirmStore.getState().pending?.resolve(false)
  return new Promise((resolve) => useConfirmStore.setState({ pending: { ...options, resolve } }))
}

// 확인 창을 그리는 곳. AppProviders에 하나만 둔다
export function ConfirmHost() {
  const t = useTranslations("common")
  const pending = useConfirmStore((s) => s.pending)

  const close = (ok: boolean) => {
    pending?.resolve(ok)
    useConfirmStore.setState({ pending: undefined })
  }

  return (
    <AlertDialog open={!!pending} onOpenChange={(open) => !open && close(false)}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{pending?.title}</AlertDialogTitle>
          {pending?.description && <AlertDialogDescription>{pending.description}</AlertDialogDescription>}
        </AlertDialogHeader>
        <AlertDialogFooter>
          {/* 취소는 창을 닫고, 닫힘은 onOpenChange가 false로 답한다 */}
          <AlertDialogCancel>{pending?.cancelLabel ?? t("cancel")}</AlertDialogCancel>
          <AlertDialogAction variant={pending?.destructive ? "destructive" : "default"} onClick={() => close(true)}>
            {pending?.confirmLabel ?? t("confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
