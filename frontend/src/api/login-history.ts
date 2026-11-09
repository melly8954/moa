import type { PageRequest, PageResponse } from "@/types/api"
import type { LoginHistory, LoginHistoryQuery } from "@/types/login-history"
import { http } from "./client"

// 접속 이력 (login-history 헬퍼, 권한 LOGIN_HISTORY_READ). api.v1.loginHistory로 부른다
// 늘 최신순이다 (정렬을 받지 않는다)
export const loginHistoryApi = {
  getPage: (params?: LoginHistoryQuery & PageRequest) =>
    http.get<PageResponse<LoginHistory>>("/api/v1/admin/login-histories", { params }).then((res) => res.data),
}
