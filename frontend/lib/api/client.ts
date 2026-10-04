// API 호출은 이 폴더에서만 한다 (docs/design/conventions.md 3.3)

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"

/** 서버 오류 응답. 화면은 code로 문구(errors.<code>)를 고르고 message는 보여 주지 않는다 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(code)
  }
}

/** 응답을 받지 못한 경우의 코드. 화면은 errors.NETWORK 문구를 보여 준다 */
export const NETWORK_ERROR = "NETWORK"

let accessToken: string | null = null

/** 액세스 토큰은 메모리에만 둔다 (localStorage·쿠키에 두지 않는다) */
export function setAccessToken(token: string | null) {
  accessToken = token
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body !== undefined) {
    headers.set("Content-Type", "application/json")
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`)
  }
  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, { ...init, headers, credentials: "include" })
  } catch {
    throw new ApiError(0, NETWORK_ERROR)
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { code?: string } | null
    throw new ApiError(response.status, body?.code ?? "S001")
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

/** 오류에서 코드를 꺼낸다. ApiError가 아니면 S001 */
export function errorCode(error: unknown): string {
  return error instanceof ApiError ? error.code : "S001"
}
