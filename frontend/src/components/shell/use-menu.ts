"use client"

import { useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { menuIcon } from "@/components/menu/icons"
import { useHasNewNotice } from "@/components/notice/use-new-notice"
import { useMyPermissions } from "@/lib/me"
import { STALE_TIME } from "@/lib/query"
import { useAuthStore } from "@/stores/auth"
import type { MyMenu } from "@/types/menu"
import { MENU, type MenuItem, toMenuItems, visibleMenu } from "./menu"

// 셸이 그릴 메뉴 (menu 헬퍼가 바꾼 파일)
//   moa: 관리 셸과 사용자 셸이 메뉴를 나눠 쓴다 (specs/001-admin-menus FR-005, research R5. 키트로 옮길 것)
//   1) 관리 셸: 서버 메뉴 (/menus/me, 관리자가 메뉴 관리 화면에서 정한 것. 이름은 번역하지 않는다)
//   2) 사용자 셸: 코드에 둔 메뉴 (menu.ts, 이름은 messages의 menu 키를 번역)
// 서버 메뉴는 이미 내 권한으로 걸러져 온다. 기준 데이터라 5분 동안 다시 묻지 않는다
export const MY_MENUS_QUERY_KEY = ["me", "menus"] as const

function fromServer(menus: MyMenu[]): MenuItem[] {
  return menus.map((m) => ({
    key: `menu-${m.id}`,
    label: m.name,
    href: m.path ?? undefined,
    icon: menuIcon(m.icon),
    children: m.children.length > 0 ? fromServer(m.children) : undefined,
  }))
}

// 내 관리 메뉴 (서버 메뉴). 로그인하지 않았으면 묻지 않는다
export function useMyMenus() {
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  return useQuery({
    queryKey: MY_MENUS_QUERY_KEY,
    queryFn: api.v1.menu.mine,
    enabled: signedIn,
    staleTime: STALE_TIME.REFERENCE,
  })
}

// 관리 셸 메뉴: 관리 메뉴만 (FR-001)
export function useAdminMenu(): MenuItem[] {
  return fromServer(useMyMenus().data ?? [])
}

// 사용자 셸 메뉴: 홈, 공지
export function useUserMenu(): MenuItem[] {
  const t = useTranslations("menu")
  const tn = useTranslations("notice")
  const hasNewNotice = useHasNewNotice()
  const permissions = useMyPermissions().data
  const local = toMenuItems(
    visibleMenu(MENU, (key) => permissions?.has(key) ?? false),
    (label) => t(label),
  )
  // moa 006: "공지" 메뉴 옆 새 공지 점 (specs/006-notices FR-010, research R12)
  return local.map((item) => (item.key === "notices" && hasNewNotice ? { ...item, badge: tn("newBadge") } : item))
}
