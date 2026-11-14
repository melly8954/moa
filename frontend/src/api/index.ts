import type { SignInInput } from "@/lib/schemas/sign-in"
import type { TokenResponse } from "@/types/api"
import type { Me, MyPermissions, PasswordChangeRequest } from "@/types/user"
import { http, refreshAccessToken } from "./client"
// 헬퍼가 더하는 API (apply가 아래 표시 위에 줄을 끼운다. 표시 줄은 지우지 않는다)
import { codeApi } from "./code"
import { loginHistoryApi } from "./login-history"
import { menuApi } from "./menu"
import { notificationApi } from "./notification"
import { noticeAdminApi, noticeApi } from "./notice"
import { roleApi } from "./role"
import { userAdminApi } from "./user-admin"
import { richTextApi } from "./rich-text"
// @helpers:imports

// 백엔드 API 창구. 화면은 이것만 부른다 (axios·fetch를 직접 부르지 않는다)
// 백엔드에 API가 생기면 도메인 하나씩 더한다. 표준 7종이면 resource()를 쓴다 (api/resource.ts)
export const api = {
  v1: {
    auth: {
      login: (data: SignInInput) =>
        http.post<TokenResponse>("/api/v1/auth/login", data, { skipAuth: true }).then((res) => res.data),
      refresh: refreshAccessToken,
      logout: () => http.post<void>("/api/v1/auth/logout", undefined, { skipAuth: true }).then(() => undefined),
    },
    user: {
      me: () => http.get<Me>("/api/v1/users/me").then((res) => res.data),
      myPermissions: () => http.get<MyPermissions>("/api/v1/users/me/permissions").then((res) => res.data),
      changePassword: (data: PasswordChangeRequest) =>
        http.put<void>("/api/v1/users/me/password", data).then(() => undefined),
    },
    // order: resource<OrderResponse, OrderCreateRequest, OrderUpdateRequest, OrderQuery>("/api/v1/orders"),
    code: codeApi,
    loginHistory: loginHistoryApi,
    menu: menuApi,
    notice: noticeApi,
    noticeAdmin: noticeAdminApi,
    notification: notificationApi,
    role: roleApi,
    userAdmin: userAdminApi,
    richText: richTextApi,
    // @helpers:api
  },
}
