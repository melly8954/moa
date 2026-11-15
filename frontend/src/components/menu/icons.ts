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
