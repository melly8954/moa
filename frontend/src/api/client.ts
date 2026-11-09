import axios from "axios"
import { useAuthStore } from "@/stores/auth"
import type { TokenResponse } from "@/types/api"
import { toApiError } from "./error"

// HTTP 인스턴스는 이것 하나다. 화면은 이것을 직접 쓰지 않고 api/index.ts의 api.v1.<도메인>.<동작>을 부른다
//   - 액세스 토큰을 Authorization 헤더에 싣는다 (skipAuth면 싣지 않는다)
//   - A002(액세스 토큰 만료)면 재발급을 한 번만 하고, 기다리던 요청을 새 토큰으로 다시 보낸다
//   - 재발급이 실패하면 세션을 비운다 (AuthGate가 안내 후 로그인 화면으로 보낸다)
//   - 오류는 모두 ApiError로 바꿔 던진다

declare module "axios" {
  interface AxiosRequestConfig {
    skipAuth?: boolean
    retried?: boolean
  }
}

const REFRESH_URL = "/api/v1/auth/token/refresh"

// 브라우저는 같은 출처의 /api로 보낸다 (next.config.ts가 백엔드로 넘긴다)
// 서버 컴포넌트는 프록시를 거치지 않으므로 백엔드 주소로 직접 보낸다. 서버에는 액세스 토큰이 없다
const baseURL = typeof window === "undefined" ? process.env.API_BASE_URL : undefined

export const http = axios.create({ baseURL, timeout: 15_000 })

let refreshing: Promise<string> | null = null

// 동시에 여러 요청이 만료돼도 재발급 요청은 하나만 나간다
export function refreshAccessToken(): Promise<string> {
  refreshing ??= http
    .post<TokenResponse>(REFRESH_URL, undefined, { skipAuth: true })
    .then((res) => {
      useAuthStore.getState().signIn(res.data.accessToken)
      return res.data.accessToken
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token && !config.skipAuth) config.headers.set("Authorization", `Bearer ${token}`)
  return config
})

http.interceptors.response.use(undefined, async (error: unknown) => {
  const apiError = toApiError(error)
  const config = axios.isAxiosError(error) ? error.config : undefined
  if (apiError.code === "A002" && config && !config.retried && !config.skipAuth) {
    try {
      await refreshAccessToken()
    } catch (refreshError) {
      useAuthStore.getState().expire()
      throw toApiError(refreshError)
    }
    return http({ ...config, retried: true })
  }
  throw apiError
})
