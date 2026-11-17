import type { QueryClient } from "@tanstack/react-query"
import { api } from "@/api"
import type { MyMenu } from "@/types/menu"
import { MY_MENUS_QUERY_KEY } from "./use-menu"

// 관리 셸 첫 화면. 볼 수 있는 첫 관리 메뉴로 보낸다 (app/(admin)/admin/page.tsx)
export const ADMIN_HOME = "/admin"

// 로그인 뒤 첫 화면 (specs/001-admin-menus FR-006, research R6)
// 관리 메뉴(/menus/me)가 하나라도 있으면 관리 셸(/admin → 첫 관리 메뉴), 없으면 사용자 셸 첫 화면
// 로그인 화면에서 로그인한 때와, 첫 화면(/)을 열어 리프레시 토큰으로 로그인이 이어진 때(AuthGate) 함께 쓴다

// 위에서부터 처음 만나는 주소 있는 메뉴
export function firstMenuPath(menus: MyMenu[]): string | undefined {
  for (const menu of menus) {
    if (menu.path) return menu.path
    const child = firstMenuPath(menu.children)
    if (child) return child
  }
  return undefined
}

export function landingPath(menus: MyMenu[]): string {
  return menus.length > 0 ? ADMIN_HOME : "/"
}

// 내 관리 메뉴를 읽어 첫 화면을 정한다. 메뉴를 읽지 못하면 사용자 셸 첫 화면 (권한은 화면마다 API가 막는다)
export async function fetchLandingPath(queryClient: QueryClient): Promise<string> {
  const menus = await queryClient
    .fetchQuery({ queryKey: MY_MENUS_QUERY_KEY, queryFn: api.v1.menu.mine })
    .catch((): MyMenu[] => [])
  return landingPath(menus)
}
