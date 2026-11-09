import axios from "axios"
import type { ErrorResponse, FieldError } from "@/types/api"

// 백엔드에 닿지 못했거나(연결 실패·시간 초과) 오류 응답 형식이 아닐 때 쓰는 프론트 전용 코드
export const NETWORK_ERROR = "NETWORK"
export const UNKNOWN_ERROR = "UNKNOWN"

// 모든 API 오류는 이 형태로 바뀌어 던져진다 (api/client.ts)
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly body?: ErrorResponse,
  ) {
    super(body?.message ?? code)
    this.name = "ApiError"
  }

  get traceId(): string | undefined {
    return this.body?.traceId
  }

  get fieldErrors(): FieldError[] {
    return this.body?.errors ?? []
  }

  get details(): Record<string, string | number> {
    return this.body?.details ?? {}
  }
}

function isErrorResponse(value: unknown): value is ErrorResponse {
  return typeof value === "object" && value !== null && typeof (value as ErrorResponse).code === "string"
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    const { response } = error
    if (!response) return new ApiError(0, NETWORK_ERROR)
    if (isErrorResponse(response.data)) return new ApiError(response.status, response.data.code, response.data)
    return new ApiError(response.status, response.status >= 500 ? "S001" : UNKNOWN_ERROR)
  }
  return new ApiError(0, UNKNOWN_ERROR)
}
