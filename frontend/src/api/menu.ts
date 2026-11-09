import type { AdminMenu, MenuCreateRequest, MenuModifyRequest, MenuOrder, MyMenu } from "@/types/menu"
import { http } from "./client"

// 메뉴 (menu 헬퍼). api.v1.menu로 부른다
//   mine: 내 메뉴 (로그인한 사용자) / admin: 메뉴 관리 화면 (MENU_MANAGE)
export const menuApi = {
  mine: () => http.get<MyMenu[]>("/api/v1/menus/me").then((res) => res.data),
  admin: {
    tree: () => http.get<AdminMenu[]>("/api/v1/admin/menus").then((res) => res.data),
    create: (data: MenuCreateRequest) => http.post<AdminMenu>("/api/v1/admin/menus", data).then((res) => res.data),
    modify: (id: number, data: MenuModifyRequest) =>
      http.put<AdminMenu>(`/api/v1/admin/menus/${id}`, data).then((res) => res.data),
    // 순서를 한 번에 바꾼다. 하나라도 틀리면 아무것도 바뀌지 않는다
    order: (orders: MenuOrder[]) => http.put<void>("/api/v1/admin/menus/order", orders).then(() => undefined),
    remove: (id: number) => http.delete<void>(`/api/v1/admin/menus/${id}`).then(() => undefined),
  },
}
