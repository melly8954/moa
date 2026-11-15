"use client"

import { useTranslations } from "next-intl"
import { confirm } from "@/components/kit/confirm-dialog"
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { cn } from "@/lib/utils"

// 관리 화면 드로어 (moa, specs/001-admin-menus FR-012, research R21). 목록에서 행을 누르면 오른쪽에서 열린다
//   넓은 폭은 오른쪽에 너비 32rem, 좁은 폭(사이드바가 서랍이 되는 폭)은 화면 전체를 덮는다
//   dirty가 true인 채로 닫으려 하면 닫을지 묻는다 (저장하지 않은 변경). 드로어 안의 취소 단추는 useDrawerClose로 닫는다
//   본문은 스크롤되고 footer(저장·취소 등)는 아래에 붙는다
//   <SideDrawer open={!!editing} onOpenChange={…} title={…} dirty={form.formState.isDirty} footer={<Button>저장</Button>}>…</SideDrawer>
// 드로어를 닫는 함수. 저장하지 않은 변경이 있으면 먼저 묻는다. 드로어 안의 "취소" 단추도 이것으로 닫는다
//   const close = useDrawerClose(form.formState.isDirty, onOpenChange)
export function useDrawerClose(dirty: boolean, onOpenChange: (open: boolean) => void) {
  const t = useTranslations("unsaved")
  return async () => {
    if (dirty) {
      const leave = await confirm({
        title: t("title"),
        description: t("description"),
        confirmLabel: t("leave"),
        cancelLabel: t("stay"),
      })
      if (!leave) return
    }
    onOpenChange(false)
  }
}

export function SideDrawer({
  open,
  onOpenChange,
  title,
  description,
  dirty = false,
  footer,
  children,
  className,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: React.ReactNode
  description?: React.ReactNode
  dirty?: boolean
  footer?: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  const close = useDrawerClose(dirty, onOpenChange)
  const change = (next: boolean) => (next ? onOpenChange(true) : close())

  return (
    <Sheet open={open} onOpenChange={(next) => void change(next)}>
      <SheetContent
        side="right"
        className={cn(
          "gap-0 data-[side=right]:w-full data-[side=right]:sm:max-w-none data-[side=right]:md:max-w-lg",
          className,
        )}
      >
        <SheetHeader className="border-b pr-12">
          <SheetTitle>{title}</SheetTitle>
          {description && <SheetDescription>{description}</SheetDescription>}
        </SheetHeader>
        <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">{children}</div>
        {footer && <SheetFooter className="flex-row justify-end border-t">{footer}</SheetFooter>}
      </SheetContent>
    </Sheet>
  )
}
