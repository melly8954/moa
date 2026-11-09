import { useTranslations } from "next-intl"
import { useMemo } from "react"
import { type ApiError, toApiError } from "@/api/error"

// 에러 코드 → 화면 문구. 문구는 messages/ko.json의 errors에 있다
// 서버 message는 로그·API 문서용이라 화면에 쓰지 않는다
// 기본 코드는 CONTRACT.md 4절 표와 같다. 프로젝트가 더한 코드의 문구는 errors에 더한다
// 문구에 끼울 값은 오류 응답의 details에서 받는다 (예: 프로젝트 코드 B101: "{limit}건까지 담을 수 있어요.")
type Details = Record<string, string | number>
type ErrorKey = Parameters<ReturnType<typeof useTranslations<"errors">>>[0]

// 돌려주는 함수는 언어가 바뀔 때만 새로 만든다 (effect 의존성에 넣어도 된다)
export function useErrorMessages() {
  const t = useTranslations("errors")
  return useMemo(() => {
    const errorMessage = (code: string, details: Details = {}): string =>
      t.has(code as ErrorKey) ? t(code as ErrorKey, details) : t("fallback")

    // 화면에 보일 문구. S001(서버 오류)은 문의할 때 쓰도록 traceId를 함께 준다
    const describeError = (error: unknown): { message: string; traceId?: string } => {
      const apiError: ApiError = toApiError(error)
      const message = errorMessage(apiError.code, apiError.details)
      return apiError.code === "S001" ? { message, traceId: apiError.traceId } : { message }
    }

    return { errorMessage, describeError }
  }, [t])
}
