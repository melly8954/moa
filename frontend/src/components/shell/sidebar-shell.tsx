"use client"

import { LogIn, PanelLeft } from "lucide-react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { useTranslations } from "next-intl"
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
import { HAS_LOCALE_CHOICE, LocaleMenu } from "./locale-menu"
import { AccountMenu } from "./account-menu"
import { isActive, type MenuItem } from "./menu"
import { ThemeMenu } from "./theme-menu"

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
        <SidebarHeader>
          <SidebarBrand homeHref={homeHref} />
        </SidebarHeader>
        <SidebarContent>
          {/* 메뉴 영역을 navigation으로 알린다 (다른 셸과 같다) */}
          <nav aria-label={t("shell.menu")}>
            <SidebarGroup>
              <SidebarGroupLabel>{t("shell.menu")}</SidebarGroupLabel>
              <SidebarMenu>
                {menu.map((item) => (
                  <SidebarMenuItem key={item.key}>
                    <SidebarMenuButton
                      className="relative"
                      isActive={isActive(item, pathname)}
                      tooltip={item.label}
                      render={item.href ? <Link href={item.href} /> : undefined}
                    >
                      {item.icon && <item.icon />}
                      {/* 새 소식 점 (moa 006 R12): 펼치면 이름 오른쪽, 접으면 아이콘 오른쪽 위. 이름 칸이 마지막이어야 말줄임이 된다 */}
                      {item.badge && (
                        <i
                          aria-hidden
                          className="order-last ml-auto size-2 shrink-0 rounded-full bg-destructive group-data-[collapsible=icon]:absolute group-data-[collapsible=icon]:top-1 group-data-[collapsible=icon]:right-1"
                        />
                      )}
                      <span>
                        {item.label}
                        {item.badge && <span className="sr-only">, {item.badge}</span>}
                      </span>
                    </SidebarMenuButton>
                    {item.children && (
                      <SidebarMenuSub>
                        {item.children.map((child) => (
                          <SidebarMenuSubItem key={child.key}>
                            <SidebarMenuSubButton
                              isActive={isActive(child, pathname)}
                              render={child.href ? <Link href={child.href} /> : undefined}
                            >
                              <span>{child.label}</span>
                            </SidebarMenuSubButton>
                          </SidebarMenuSubItem>
                        ))}
                      </SidebarMenuSub>
                    )}
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroup>
          </nav>
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu>
            {/* 개인 알림 벨 (moa 007 US2, 키트로 옮길 것) */}
            {signedIn && (
              <SidebarMenuItem>
                <NotificationBell showLabel trigger={<SidebarMenuButton tooltip={t("notification.bell")} />} />
              </SidebarMenuItem>
            )}
            <SidebarMenuItem>
              <ThemeMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.theme")} />} />
            </SidebarMenuItem>
            {HAS_LOCALE_CHOICE && (
              <SidebarMenuItem>
                <LocaleMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.language")} />} />
              </SidebarMenuItem>
            )}
            {signedIn && (
              <SidebarMenuItem>
                <AccountMenu trigger={<SidebarMenuButton tooltip={t("shell.account")} />} />
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
          <span className="truncate font-medium">{SITE.name}</span>
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
