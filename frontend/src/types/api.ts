// 백엔드와 주고받는 공통 형식. CONTRACT.md 4절과 같아야 한다

// 오류 응답. errors는 V001일 때만, details는 화면 문구에 끼울 값이 있을 때만 온다
export type ErrorResponse = {
  code: string
  message: string
  timestamp: string
  path: string
  traceId?: string
  errors?: FieldError[]
  details?: Record<string, string | number>
}

export type FieldError = { field: string; message: string }

// 페이징 목록. page는 0부터
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// sort는 Spring 형식 "필드,asc" / "필드,desc"
export type PageRequest = { page?: number; size?: number; sort?: string | string[] }
export type SortRequest = { sort?: string | string[] }

// 로그인·재발급 응답. 웹은 리프레시 토큰을 HttpOnly 쿠키로 받으므로 refreshToken이 없다
export type TokenResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
}
