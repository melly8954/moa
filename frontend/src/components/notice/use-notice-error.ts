"use client"

import { toApiError } from "@/api/error"
import { useErrorMessages } from "@/lib/error-messages"

// 공지 API 오류 문구. 공통 오류 문구를 쓴다 (R001 없는 공지, A005 권한 없음, B001 이미 게시 등)
export function useNoticeErrorText() {
  const { describeError } = useErrorMessages()
  return (error: unknown) => describeError(toApiError(error)).message
}
