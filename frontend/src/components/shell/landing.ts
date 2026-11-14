import type { MyMenu } from "@/types/menu"

// 관리 셸 첫 화면. 볼 수 있는 첫 관리 메뉴로 보낸다 (app/(admin)/admin/page.tsx)
export const ADMIN_HOME = "/admin"

// 로그인 뒤 첫 화면 (specs/001-admin-menus FR-006, research R6)
// 관리 메뉴(/menus/me)가 하나라도 있으면 관리 셸(/admin → 첫 관리 메뉴), 없으면 사용자 셸 첫 화면

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
