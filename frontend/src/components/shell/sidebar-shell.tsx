"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useTranslations } from "next-intl"
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
} from "@/components/ui/sidebar"
import { SITE } from "@/lib/site"
import { HAS_LOCALE_CHOICE, LocaleMenu } from "./locale-menu"
import { AccountMenu } from "./account-menu"
import { isActive, screenTitle } from "./menu"
import { ThemeMenu } from "./theme-menu"
import { useMenu } from "./use-menu"

// 사이드바형 셸. 폭 768px 미만에서는 사이드바가 서랍(Sheet)으로 바뀐다
// 설정: side left|right, collapsible icon|offcanvas|none, variant sidebar|floating|inset
export function SidebarShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const menu = useMenu()
  const t = useTranslations()
  const title = screenTitle(menu, pathname)
  return (
    <SidebarProvider defaultOpen>
      <Sidebar side="left" collapsible="icon" variant="sidebar">
        <SidebarHeader>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton size="lg" render={<Link href="/" />}>
                <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                  {SITE.name.slice(0, 1).toUpperCase()}
                </div>
                <span className="truncate font-medium">{SITE.name}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
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
            <SidebarMenuItem>
              <ThemeMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.theme")} />} />
            </SidebarMenuItem>
            {HAS_LOCALE_CHOICE && (
              <SidebarMenuItem>
                <LocaleMenu showLabel trigger={<SidebarMenuButton tooltip={t("shell.language")} />} />
              </SidebarMenuItem>
            )}
            <SidebarMenuItem>
              <AccountMenu trigger={<SidebarMenuButton tooltip={t("shell.account")} />} />
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      {/* min-w-0: 넓은 표가 본문을 늘리지 않고 표 안에서 가로로 넘기게 한다 */}
      <SidebarInset className="min-w-0">
        <header className="flex h-(--header-height) shrink-0 items-center gap-2 border-b px-(--page-padding)">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 h-4" />
          {/* 위치(메뉴 이름)만 보인다. 화면 제목(h1)은 화면의 PageHeader가 둔다 */}
          {title && <span className="text-sm font-medium">{title.label}</span>}
        </header>
        <div className="flex min-w-0 flex-1 flex-col gap-(--page-gap) p-(--page-padding)">{children}</div>
      </SidebarInset>
    </SidebarProvider>
  )
}
