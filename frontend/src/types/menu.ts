// 메뉴 API의 응답·요청 (백엔드 menu 헬퍼: MyMenuResponse, MenuTreeResponse, Menu*Request)

// 내 메뉴 (사용 중이고 내 권한으로 볼 수 있는 것)
export type MyMenu = {
  id: number
  name: string
  // 그룹이면 없다
  path?: string | null
  // 아이콘 이름 (components/menu/icons.ts)
  icon?: string | null
  children: MyMenu[]
}

// 관리 화면의 메뉴 (사용 안 함 포함)
export type AdminMenu = {
  id: number
  parentId?: number | null
  name: string
  path?: string | null
  icon?: string | null
  sortOrder: number
  enabled: boolean
  // 없으면 로그인한 사용자 모두
  permission?: string | null
  children: AdminMenu[]
}

export type MenuCreateRequest = {
  parentId?: number
  name: string
  path?: string
  icon?: string
  sortOrder: number
  permission?: string
}
export type MenuModifyRequest = MenuCreateRequest & { enabled: boolean }
export type MenuOrder = { id: number; parentId?: number | null; sortOrder: number }
