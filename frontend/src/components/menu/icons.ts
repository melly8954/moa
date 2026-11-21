import { createElement } from "react"
import {
  Bell,
  BriefcaseBusiness,
  ChartColumn,
  ClipboardList,
  FileText,
  Folder,
  History,
  House,
  List,
  type LucideIcon,
  type LucideProps,
  Mail,
  Megaphone,
  Menu,
  Package,
  Settings,
  ShieldCheck,
  ShoppingCart,
  Users,
} from "lucide-react"

// 메뉴 아이콘 이름 → 아이콘. 서버는 이름(문자열)만 저장하고 바꾸는 것은 프론트가 한다
// 메뉴 관리 화면의 아이콘 고르기도 이 목록을 쓴다. 아이콘을 더하려면 여기에 더한다
// 이름은 백엔드 시드(menu 헬퍼, admin 프리셋)와 맞춘다: menu, list, history, shield, users, settings
export const MENU_ICONS: Record<string, LucideIcon> = {
  home: House,
  menu: Menu,
  list: List,
  history: History,
  shield: ShieldCheck,
  users: Users,
  settings: Settings,
  folder: Folder,
  file: FileText,
  clipboard: ClipboardList,
  chart: ChartColumn,
  bell: Bell,
  mail: Mail,
  package: Package,
  cart: ShoppingCart,
  // moa: 사용자 메뉴 "공지" (specs/001-admin-menus FR-007)
  megaphone: Megaphone,
  // moa: 관리 메뉴 묶음 "운영" (specs/001-admin-menus FR-001)
  briefcase: BriefcaseBusiness,
}

// 모르는 이름이면 아이콘 없음 (셸이 이름 첫 글자를 보인다)
export function menuIcon(name?: string | null): LucideIcon | undefined {
  return name ? MENU_ICONS[name] : undefined
}

// 이름으로 메뉴 아이콘을 그린다. 없는 이름이면 fallback, 그것도 없으면 그리지 않는다
// (렌더 중에 아이콘 컴포넌트를 변수로 꺼내 쓰지 않으려고 createElement로 그린다)
export function MenuIcon({
  name,
  fallback,
  ...props
}: { name?: string | null; fallback?: LucideIcon } & Omit<LucideProps, "name">) {
  const icon = menuIcon(name) ?? fallback
  return icon ? createElement(icon, props) : null
}
