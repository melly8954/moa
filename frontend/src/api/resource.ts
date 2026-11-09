import type { PageRequest, PageResponse, SortRequest } from "@/types/api"
import { http } from "./client"

type Id = number | string

// 목록·단건·등록·수정·삭제 표준 7종. 모든 도메인이 같은 이름과 모양으로 부른다
//   예: order: resource<OrderResponse, OrderCreateRequest, OrderUpdateRequest, OrderQuery>("/api/v1/orders")
// 백엔드 경로가 이 모양과 다르면 resource를 쓰지 않고 api/index.ts에 직접 적는다
export function resource<TItem, TCreate = never, TUpdate = TCreate, TQuery extends object = object>(path: string) {
  return {
    getPage: (params?: TQuery & PageRequest) => http.get<PageResponse<TItem>>(path, { params }).then((res) => res.data),
    getList: (params?: TQuery & SortRequest) => http.get<TItem[]>(`${path}/all`, { params }).then((res) => res.data),
    get: (id: Id) => http.get<TItem>(`${path}/${id}`).then((res) => res.data),
    create: (data: TCreate) => http.post<TItem>(path, data).then((res) => res.data),
    modify: (id: Id, data: TUpdate) => http.put<TItem>(`${path}/${id}`, data).then((res) => res.data),
    remove: (id: Id) => http.delete<void>(`${path}/${id}`).then(() => undefined),
    removeList: (idList: Id[]) => http.delete<void>(path, { data: idList }).then(() => undefined),
  }
}
