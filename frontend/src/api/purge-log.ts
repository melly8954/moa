import type { PageRequest, PageResponse } from "@/types/api"
import type { PurgeLog, PurgeLogQuery } from "@/types/purge-log"
import { http } from "./client"

// 파기 이력 (moa, specs/013-purge-log, 권한 PURGE_LOG_READ). api.v1.purgeLog로 부른다
// 늘 최신순이다 (정렬을 받지 않는다)
export const purgeLogApi = {
  getPage: (params?: PurgeLogQuery & PageRequest) =>
    http.get<PageResponse<PurgeLog>>("/api/v1/admin/purge-logs", { params }).then((res) => res.data),
}
