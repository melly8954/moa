import { House, type LucideIcon, Megaphone } from "lucide-react"
import type ko from "../../../messages/ko.json"
// 헬퍼가 더하는 메뉴 아이콘 (apply가 아래 표시 위에 줄을 끼운다. 표시 줄은 지우지 않는다)
// @helpers:imports

// 셸 메뉴 정의. 화면을 만들면 여기에 더한다. 하위 메뉴는 children (사이드바형은 펼쳐 보이고, 상단 메뉴형은 서랍에서 보인다)
// label은 messages/ko.json의 menu 키다. useMenu가 번역해 셸에 넘긴다
// permission: 그 권한이 있는 사용자에게만 보인다 (없으면 모두). 숨길 뿐이고 화면·API는 따로 막는다
export type MenuLabel = keyof (typeof ko)["menu"]
export type MenuEntry = {
  label: MenuLabel
  href?: string
  icon?: LucideIcon
  permission?: string
  children?: MenuEntry[]
}

export const MENU: MenuEntry[] = [
  { label: "home", href: "/", icon: House },
  // 공지 (specs/006-notices): 로그인 없이 보는 화면이지만 로그인한 모두에게 메뉴로 보인다
  { label: "notices", href: "/notices", icon: Megaphone },
  // @helpers:menu
]

// 셸이 그리는 메뉴: key는 고유값, label은 보일 글자 (번역했거나 서버가 준 이름)
export type MenuItem = {
  key: string
  label: string
  href?: string
  icon?: LucideIcon
  children?: MenuItem[]
}

type Tree<T> = { href?: string; permission?: string; children?: T[] }

// 내 권한으로 볼 수 있는 메뉴만 남긴다. 하위 메뉴가 모두 빠지고 주소도 없는 묶음은 뺀다
export function visibleMenu<T extends Tree<T>>(menu: T[], has: (permission: string) => boolean): T[] {
  return menu.flatMap((item) => {
    if (item.permission && !has(item.permission)) return []
    if (!item.children) return [item]
    const children = visibleMenu(item.children, has)
    if (children.length === 0 && !item.href) return []
    return [{ ...item, children }]
  })
}

// menu.ts 정의 → 셸 메뉴. translate는 menu 키를 글자로 바꾼다
export function toMenuItems(entries: MenuEntry[], translate: (label: MenuLabel) => string): MenuItem[] {
  return entries.map((entry) => ({
    key: entry.label,
    label: translate(entry.label),
    href: entry.href,
    icon: entry.icon,
    children: entry.children && toMenuItems(entry.children, translate),
  }))
}

// 지금 화면이 이 메뉴(또는 하위 메뉴)인가. "/"는 정확히 같을 때만, 나머지는 하위 경로까지 포함한다
export function isActive(item: MenuItem, pathname: string): boolean {
  if (
    item.href &&
    (item.href === "/" ? pathname === "/" : pathname === item.href || pathname.startsWith(`${item.href}/`))
  )
    return true
  return item.children?.some((child) => isActive(child, pathname)) ?? false
}

// 헤더에 보일 위치. 가장 깊이 맞는 메뉴 (맞는 메뉴가 없으면 undefined)
export function screenTitle(menu: MenuItem[], pathname: string): MenuItem | undefined {
  for (const item of menu) {
    if (!isActive(item, pathname)) continue
    return (item.children && screenTitle(item.children, pathname)) || item
  }
  return undefined
}
