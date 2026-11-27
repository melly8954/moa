import type { PageRequest, PageResponse } from "@/types/api"
import type { AdminBanner, AdminBannerListItem, BannerSaveRequest } from "@/types/banner"
import type { ExposureQuery } from "@/types/marketing"
import { http } from "./client"

// 배너 관리 (specs/016-banner-popup, 권한 BANNER_MANAGE). api.v1.bannerAdmin으로 부른다
//   이미지는 api.v1.richText.uploadImage로 먼저 올리고 받은 url을 넣는다 (research R1)
const base = "/api/v1/admin/banners"

export const bannerAdminApi = {
  getPage: (params?: ExposureQuery & PageRequest) =>
    http.get<PageResponse<AdminBannerListItem>>(base, { params }).then((res) => res.data),
  get: (id: number) => http.get<AdminBanner>(`${base}/${id}`).then((res) => res.data),
  create: (data: BannerSaveRequest) => http.post<AdminBanner>(base, data).then((res) => res.data),
  modify: (id: number, data: BannerSaveRequest) => http.put<AdminBanner>(`${base}/${id}`, data).then((res) => res.data),
  remove: (id: number) => http.delete<void>(`${base}/${id}`).then(() => undefined),
  setEnabled: (ids: number[], enabled: boolean) =>
    http.put<void>(`${base}/enabled`, { ids, enabled }).then(() => undefined),
  bulkDelete: (ids: number[]) => http.post<void>(`${base}/bulk-delete`, { ids }).then(() => undefined),
}
