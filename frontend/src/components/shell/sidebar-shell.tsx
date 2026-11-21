"use client"

import { ChevronRight, Lock, LogIn, PanelLeft } from "lucide-react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { LinkButton } from "@/components/kit/link-button"
import { NotificationBell } from "@/components/notification/notification-bell"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar"
import { useIsMobile } from "@/hooks/use-mobile"
import { signInPath } from "@/lib/next-path"
import { cn } from "@/lib/utils"
import { SITE } from "@/lib/site"
import { useAuthStore } from "@/stores/auth"
import { useEffect } from "react"
import { HAS_LOCALE_CHOICE, LocaleMenu } from "./locale-menu"
import { AccountBar } from "./account-bar"
import { isActive, type MenuItem } from "./menu"
import { ThemeMenu } from "./theme-menu"
import { useOpenMenus } from "./use-open-menus"

// 사이드바형 셸. 폭 768px 미만에서는 사이드바가 서랍(Sheet)으로 바뀐다
// 설정: side left|right, collapsible icon|offcanvas|none, variant sidebar|floating|inset
// moa: 메뉴와 로고 주소를 받아 관리 셸·사용자 셸이 함께 쓴다 (specs/001-admin-menus FR-005, research R5. 키트로 옮길 것)
//   로그인하지 않았으면 계정 줄에 벨·계정 메뉴 대신 로그인 단추를 둔다 (사용자 셸의 공지 화면)
//   본문 머리 줄은 좁은 폭(서랍)에서만 둔다. 넓은 폭의 접기 단추는 로고 줄에 있다 (001 FR-005, research R12. 키트로 옮길 것)
export function SidebarShell({
  menu,
  homeHref,
  children,
}: {
  menu: MenuItem[]
  homeHref: string
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const t = useTranslations()
  const status = useAuthStore((s) => s.status)
  const signedIn = status === "signedIn"
  // 좁은 폭은 사이드바가 서랍이라 계정 줄 벨이 숨는다. 머리에도 벨을 둔다 (moa 007 FR-005, 키트로 옮길 것)
  const isMobile = useIsMobile()
  return (
    <SidebarProvider defaultOpen>
      <Sidebar side="left" collapsible="icon" variant="sidebar">
        {/* 머리·메뉴·바닥 사이를 선으로 나눈다 (001 FR-005, R24. 키트로 옮길 것) */}
        <SidebarHeader className="border-b border-sidebar-border">
          <SidebarBrand homeHref={homeHref} />
        </SidebarHeader>
        <SidebarContent>
          {/* 메뉴 영역을 navigation으로 알린다 (다른 셸과 같다) */}
          <nav aria-label={t("shell.menu")}>
            <SidebarGroup>
              <SidebarGroupLabel>{t("shell.menu")}</SidebarGroupLabel>
              <SidebarNav menu={menu} pathname={pathname} />
            </SidebarGroup>
          </nav>
        </SidebarContent>
        <SidebarFooter className="border-t border-sidebar-border">
          {/* 로그인했으면 계정 줄 한 줄: 이름·아이디, 알림 벨, 톱니(화면 모드·설정·비밀번호 변경), 로그아웃 (moa 001 FR-005, 2026-11-21) */}
          {signedIn && <AccountBar />}
          <SidebarMenu>
            {!signedIn && (
              <SidebarMenuItem>
                <ThemeMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.theme")} />} />
              </SidebarMenuItem>
            )}
            {!signedIn && HAS_LOCALE_CHOICE && (
              <SidebarMenuItem>
                <LocaleMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.language")} />} />
              </SidebarMenuItem>
            )}
            {status === "signedOut" && (
              <SidebarMenuItem>
                <SidebarMenuButton tooltip={t("shell.signIn")} render={<Link href={signInPath(pathname)} />}>
                  <LogIn />
                  <span>{t("shell.signIn")}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            )}
          </SidebarMenu>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      {/* min-w-0: 넓은 표가 본문을 늘리지 않고 표 안에서 가로로 넘기게 한다 */}
      <SidebarInset className="min-w-0">
        {/* 좁은 폭만: 서랍 단추, 사이트 이름, 벨 또는 로그인. 화면 이름은 본문 제목(PageHeader)이 둔다 */}
        <header className="flex h-(--header-height) shrink-0 items-center gap-2 border-b px-(--page-padding) md:hidden">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 h-4" />
          <Link href={homeHref} className="text-sm font-medium">
            {SITE.name}
          </Link>
          {isMobile && signedIn && (
            <div className="ml-auto">
              <NotificationBell trigger={<Button variant="ghost" size="icon" />} />
            </div>
          )}
          {/* 좁은 폭에서 로그인하지 않았으면 서랍을 열지 않아도 로그인할 수 있게 머리에도 둔다 */}
          {isMobile && status === "signedOut" && (
            <LinkButton href={signInPath(pathname)} variant="outline" size="sm" className="ml-auto">
              {t("shell.signIn")}
            </LinkButton>
          )}
        </header>
        <div className="flex min-w-0 flex-1 flex-col gap-(--page-gap) p-(--page-padding)">{children}</div>
      </SidebarInset>
    </SidebarProvider>
  )
}

// 사이드바 메뉴. 하위 메뉴가 있는 묶음(주소 없음)은 행을 눌러 펴고 접는다 (moa 001 FR-005, research R24. 키트로 옮길 것)
//   펴짐 = 브라우저에 기억한 값 ?? 지금 화면이 그 안인가. 자동으로 펴진 묶음도 펴진 채로 기억한다 (US1-AC8)
//   접힌 하위 목록은 그리지 않는다. 아이콘만 남게 접혔을 때 묶음을 누르면 사이드바를 펼치고 그 묶음을 편다
function SidebarNav({ menu, pathname }: { menu: MenuItem[]; pathname: string }) {
  const t = useTranslations()
  const { state, isMobile, setOpen } = useSidebar()
  const { openMenus, setMenuOpen, rememberOpen } = useOpenMenus()
  const isGroup = (item: MenuItem) => !item.href && !!item.children?.length
  const activeGroup = menu.find((item) => isGroup(item) && isActive(item, pathname))?.key
  useEffect(() => {
    if (activeGroup) rememberOpen(activeGroup)
  }, [activeGroup, rememberOpen])

  // moa: 권한 없는 메뉴를 누르면 지금 화면에 머물고 토스트로 알린다 (001 FR-002, 2026-11-22)
  const noAccess = () => toast.error(t("shell.noAccess"))

  return (
    <SidebarMenu>
      {menu.map((item) => {
        const group = isGroup(item)
        const open = group ? (openMenus[item.key] ?? isActive(item, pathname)) : true
        const subId = `sidebar-sub-${item.key}`
        const toggle = () => {
          if (state === "collapsed" && !isMobile) {
            setOpen(true)
            setMenuOpen(item.key, true)
          } else setMenuOpen(item.key, !open)
        }
        // moa: 권한 없는 화면 메뉴는 링크가 아니라 토스트를 띄우는 단추다 (001 FR-002, 2026-11-22)
        const lockedScreen = !group && item.locked
        return (
          <SidebarMenuItem key={item.key}>
            <SidebarMenuButton
              className={cn("relative", item.locked && LOCKED)}
              isActive={!lockedScreen && isActive(item, pathname)}
              tooltip={item.label}
              render={item.href && !lockedScreen ? <Link href={item.href} /> : undefined}
              {...(lockedScreen && { onClick: noAccess })}
              {...(group && { "aria-expanded": open, "aria-controls": open ? subId : undefined, onClick: toggle })}
            >
              {item.icon && <item.icon />}
              {lockedScreen && <LockMark />}
              {/* 새 소식 점 (moa 006 R12): 펼치면 이름 오른쪽, 접으면 아이콘 오른쪽 위. 이름 칸이 마지막이어야 말줄임이 된다 */}
              {item.badge && (
                <i
                  aria-hidden
                  className="order-last ml-auto size-2 shrink-0 rounded-full bg-destructive group-data-[collapsible=icon]:absolute group-data-[collapsible=icon]:top-1 group-data-[collapsible=icon]:right-1"
                />
              )}
              {group && (
                <ChevronRight
                  aria-hidden
                  data-open={open || undefined}
                  className={cn(
                    "order-last text-muted-foreground transition-transform group-data-[collapsible=icon]:hidden data-open:rotate-90",
                    !item.badge && "ml-auto",
                  )}
                />
              )}
              <span>
                {item.label}
                {item.badge && <span className="sr-only">, {item.badge}</span>}
                {lockedScreen && <span className="sr-only">, {t("shell.locked")}</span>}
              </span>
            </SidebarMenuButton>
            {item.children && open && (
              <SidebarMenuSub id={subId}>
                {item.children.map((child) => (
                  <SidebarMenuSubItem key={child.key}>
                    <SidebarMenuSubButton
                      className={cn("w-full", child.locked && LOCKED)}
                      isActive={!child.locked && isActive(child, pathname)}
                      render={
                        child.locked ? (
                          <button type="button" onClick={noAccess} />
                        ) : child.href ? (
                          <Link href={child.href} />
                        ) : undefined
                      }
                    >
                      {/* moa: 하위 메뉴도 아이콘이 있으면 보인다 (specs/001-admin-menus FR-005, 2026-11-21) */}
                      {child.icon && <child.icon />}
                      {child.locked && <LockMark />}
                      <span>
                        {child.label}
                        {child.locked && <span className="sr-only">, {t("shell.locked")}</span>}
                      </span>
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                ))}
              </SidebarMenuSub>
            )}
          </SidebarMenuItem>
        )
      })}
    </SidebarMenu>
  )
}

// moa: 권한 없는 메뉴 모양. 글자와 아이콘을 흐리게 (001 FR-002, 2026-11-22)
//   aria-disabled는 쓰지 않는다: 사이드바 단추는 aria-disabled면 클릭을 막아(pointer-events: none) 토스트를 띄울 수 없다
const LOCKED = "text-sidebar-foreground/50 hover:text-sidebar-foreground/70 [&_svg]:text-sidebar-foreground/50"

// 자물쇠: 메뉴명 길이와 상관없이 줄 오른쪽 끝에 두어 같은 단계의 메뉴끼리 같은 열에 선다. 아이콘만 남게 접으면 숨긴다
//   화면 읽기 프로그램에는 이름 칸 안의 ", 권한 없음"으로 메뉴명 뒤에 읽힌다 (새 소식 점과 같은 방식)
function LockMark() {
  return <Lock aria-hidden className="order-last ml-auto size-3.5! shrink-0 group-data-[collapsible=icon]:hidden" />
}

// 사이드바 로고 줄. 펼쳤을 때: 로고(첫 화면 링크)와 접기 단추. 아이콘만 남게 접었을 때: 로고 자리가 펼치기 단추
//   (평소 로고 글자, 마우스를 올리거나 키보드 초점이 닿으면 펼치기 아이콘). 서랍(좁은 폭)에는 접기 단추가 없다
function SidebarBrand({ homeHref }: { homeHref: string }) {
  const t = useTranslations("shell")
  const { state, isMobile, toggleSidebar } = useSidebar()
  const initial = SITE.name.slice(0, 1).toUpperCase()
  const logo =
    "flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground"
  if (state === "collapsed" && !isMobile) {
    return (
      <SidebarMenu>
        <SidebarMenuItem>
          <SidebarMenuButton
            size="lg"
            aria-label={t("expandSidebar")}
            tooltip={t("expandSidebar")}
            onClick={toggleSidebar}
          >
            <div className={cn(logo, "group-hover/menu-button:hidden group-focus-visible/menu-button:hidden")}>
              {initial}
            </div>
            <div className="hidden size-8 items-center justify-center group-hover/menu-button:flex group-focus-visible/menu-button:flex">
              <PanelLeft />
            </div>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    )
  }
  return (
    <SidebarMenu>
      <SidebarMenuItem className="flex items-center gap-1">
        <SidebarMenuButton size="lg" className="min-w-0 flex-1" render={<Link href={homeHref} />}>
          <div className={logo}>{initial}</div>
          <span className="truncate font-semibold">{SITE.name}</span>
        </SidebarMenuButton>
        {!isMobile && (
          <Button variant="ghost" size="icon-sm" aria-label={t("collapseSidebar")} onClick={toggleSidebar}>
            <PanelLeft />
          </Button>
        )}
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
