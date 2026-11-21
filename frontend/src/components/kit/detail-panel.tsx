"use client"

import { useTranslations } from "next-intl"
import { createContext, useContext, useEffect, useMemo, useRef, useSyncExternalStore } from "react"
import { confirm } from "@/components/kit/confirm-dialog"
import { SideDrawer } from "@/components/kit/side-drawer"

// 관리 화면 오른쪽 상세 패널 (specs/004-admin-codes FR-005, research R12). 넓은 폭에서 드로어 대신 화면을 좌 트리 · 우 상세로 나눈다
//   공통코드와 메뉴 관리가 함께 쓴다 (specs/001-admin-menus FR-010, research R25). 키트로 옮길 것
//   SideDrawer와 같은 props를 받아 드로어 자리에 그대로 쓴다. open이 false면 아무것도 그리지 않는다
//   dirty는 화면이 받아 둔다. 다른 행을 고르기 전에 저장하지 않은 변경을 묻는 데 쓴다 (usePanelGuard)

type Guard = { setDirty: (dirty: boolean) => void }
export const PanelGuardContext = createContext<Guard | null>(null)

// 화면에서 쓴다: <PanelGuardContext.Provider value={guard}>로 패널을 감싸고, 다른 것을 고르기 전에 confirmLeave()를 부른다
export function usePanelGuard() {
  const t = useTranslations("unsaved")
  const dirty = useRef(false)
  const guard = useMemo<Guard>(
    () => ({
      setDirty: (value) => {
        dirty.current = value
      },
    }),
    [],
  )
  const confirmLeave = async () => {
    if (!dirty.current) return true
    const leave = await confirm({
      title: t("title"),
      description: t("description"),
      confirmLabel: t("leave"),
      cancelLabel: t("stay"),
    })
    if (leave) dirty.current = false
    return leave
  }
  return { guard, confirmLeave }
}

export function DetailPanel({
  open,
  title,
  description,
  dirty = false,
  footer,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: React.ReactNode
  description?: React.ReactNode
  dirty?: boolean
  footer?: React.ReactNode
  children: React.ReactNode
}) {
  const guard = useContext(PanelGuardContext)
  useEffect(() => {
    if (!guard) return
    guard.setDirty(open && dirty)
    return () => guard.setDirty(false)
  }, [guard, open, dirty])

  if (!open) return null
  return (
    // 넓은 폭: 오른쪽 칸 높이를 채운다. 본문만 스크롤되고 저장 줄은 아래에 붙는다
    <section
      aria-label={typeof title === "string" ? title : undefined}
      className="flex flex-col rounded-lg border lg:h-full"
    >
      <header className="flex flex-col gap-1 border-b p-4">
        <h2 className="font-medium">{title}</h2>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </header>
      <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">{children}</div>
      {footer && <footer className="flex flex-row justify-end gap-2 border-t p-4">{footer}</footer>}
    </section>
  )
}

// 넓은 폭(lg, 1024px 이상)인가. 두 칸 화면은 이 폭부터다. 서버에서 그릴 때는 넓은 폭으로 본다
const WIDE = "(min-width: 1024px)"
export function useIsWide() {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(WIDE)
      mql.addEventListener("change", onChange)
      return () => mql.removeEventListener("change", onChange)
    },
    () => window.matchMedia(WIDE).matches,
    () => true,
  )
}

// 넓은 폭은 오른쪽 패널, 좁은 폭은 드로어 (SideDrawer와 같은 props)
export function ResponsivePanel(props: React.ComponentProps<typeof DetailPanel>) {
  const wide = useIsWide()
  return wide ? <DetailPanel {...props} /> : <SideDrawer {...props} />
}
