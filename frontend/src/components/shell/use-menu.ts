"use client"

import { useQuery } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { menuIcon } from "@/components/menu/icons"
import { useHasNewNotice } from "@/components/notice/use-new-notice"
import { STALE_TIME } from "@/lib/query"
import { useAuthStore } from "@/stores/auth"
import type { MyMenu } from "@/types/menu"
import type { MenuItem } from "./menu"

// 셸이 그릴 메뉴 (menu 헬퍼가 바꾼 파일)
//   moa: 관리 셸과 사용자 셸이 메뉴를 나눠 쓴다 (specs/001-admin-menus FR-005, research R5. 키트로 옮길 것)
//   1) 관리 셸: 서버 메뉴 (/menus/me, 관리자가 메뉴 관리 화면에서 정한 것. 이름은 번역하지 않는다)
//   2) 사용자 셸: 서버 사용자 메뉴 (/menus/user, 대상 USER. 로그인하지 않아도 부른다. FR-007)
// 서버 메뉴는 이미 내 권한으로 걸러져 온다. 기준 데이터라 5분 동안 다시 묻지 않는다
export const MY_MENUS_QUERY_KEY = ["me", "menus"] as const
export const USER_MENUS_QUERY_KEY = ["menus", "user"] as const

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

// 사용자 셸 메뉴 (처음 구성: 홈, 공지). 로그인 상태가 정해진 뒤 읽고, 로그인·로그아웃하면 다시 읽는다 (권한을 건 메뉴)
export function useUserMenu(): MenuItem[] {
  const tn = useTranslations("notice")
  const hasNewNotice = useHasNewNotice()
  const status = useAuthStore((s) => s.status)
  const menus = useQuery({
    queryKey: [...USER_MENUS_QUERY_KEY, status],
    queryFn: api.v1.menu.user,
    enabled: status !== "unknown",
    staleTime: STALE_TIME.REFERENCE,
  })
  // moa 006: 주소가 /notices인 메뉴 옆 새 공지 점 (specs/006-notices FR-010, research R12)
  return fromServer(menus.data ?? []).map((item) =>
    item.href === NOTICES_PATH && hasNewNotice ? { ...item, badge: tn("newBadge") } : item,
  )
}

const NOTICES_PATH = "/notices"
