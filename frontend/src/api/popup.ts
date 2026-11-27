import type { PageRequest, PageResponse } from "@/types/api"
import type { ExposureQuery } from "@/types/marketing"
import type { AdminPopup, AdminPopupListItem, PopupSaveRequest } from "@/types/popup"
import { http } from "./client"

// 팝업 관리 (specs/016-banner-popup, 권한 POPUP_MANAGE). api.v1.popupAdmin으로 부른다
//   내용의 이미지는 공지와 같이 rich-text 편집기가 올린다
const base = "/api/v1/admin/popups"

export const popupAdminApi = {
  getPage: (params?: ExposureQuery & PageRequest) =>
    http.get<PageResponse<AdminPopupListItem>>(base, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminPopup>(`${base}/${id}`).then((res) => res.data),
  create: (data: PopupSaveRequest) => http.post<AdminPopup>(base, data).then((res) => res.data),
  modify: (id: number, data: PopupSaveRequest) => http.put<AdminPopup>(`${base}/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${base}/${id}`).then(() => undefined),
  setEnabled: (ids: number[], enabled: boolean) =>
    http.put<void>(`${base}/enabled`, { ids, enabled }).then(() => undefined),
  bulkDelete: (ids: number[]) => http.post<void>(`${base}/bulk-delete`, { ids }).then(() => undefined),
}
