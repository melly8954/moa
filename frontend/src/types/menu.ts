// 메뉴 API의 응답·요청 (백엔드 menu 헬퍼: MyMenuResponse, MenuTreeResponse, Menu*Request, ScreenResponse)

// 내 메뉴 (사용 중이고 내 권한으로 볼 수 있는 것)
export type MyMenu = {
  id: number
  name: string
  // 묶음 메뉴면 없다. 화면 메뉴는 연결 화면의 경로
  path?: string | null
  // 아이콘 이름 (components/menu/icons.ts)
  icon?: string | null
  children: MyMenu[]
}

// 메뉴 대상 (moa, specs/001-admin-menus FR-007). ADMIN은 관리 셸, USER는 사용자 셸. 하위 메뉴는 부모를 따른다
export type MenuAudience = "ADMIN" | "USER"

// 사용자 화면 메뉴의 공개 범위 (moa, FR-007). PUBLIC은 로그인하지 않은 사람까지, MEMBER는 로그인한 사용자만
export type MenuVisibility = "PUBLIC" | "MEMBER"

// 연결 화면: 메뉴에 걸 수 있는 화면. 코드에 등록하고 화면에서는 조회만 한다 (moa, FR-008)
export type Screen = {
  key: string
  name: string
  path: string
  audience: MenuAudience
  // 관리 연결 화면의 권한. 사용자 연결 화면은 없다
  permission?: { key: string; name: string } | null
  // 로그인이 필요한가. 관리 연결 화면은 언제나 true
  signInRequired: boolean
}

// 관리 화면의 메뉴 (사용 안 함 포함)
export type AdminMenu = {
  id: number
  parentId?: number | null
  name: string
  icon?: string | null
  sortOrder: number
  enabled: boolean
  audience: MenuAudience
  // 묶음 메뉴면 없다
  screenKey?: string | null
  // 묶음 메뉴이거나 없는 연결 화면이면 없다
  screen?: {
    name: string
    path: string
    permission?: string | null
    permissionName?: string | null
    signInRequired: boolean
  } | null
  // 연결 화면 키가 있는데 코드에서 빠졌다. 사이드바에 보이지 않는다
  missingScreen: boolean
  // 사용자 화면 메뉴만 있다
  visibility?: MenuVisibility | null
  children: AdminMenu[]
}

export type MenuCreateRequest = {
  parentId?: number
  name: string
  // 비우면 묶음 메뉴 (1단계만)
  screenKey?: string
  icon?: string
  sortOrder: number
  // 1단계 메뉴만 쓴다. 하위 메뉴는 부모를 따른다
  audience?: MenuAudience
  // 사용자 화면 메뉴는 필수
  visibility?: MenuVisibility
}
export type MenuModifyRequest = MenuCreateRequest & { enabled: boolean }
export type MenuOrder = { id: number; parentId?: number | null; sortOrder: number }
