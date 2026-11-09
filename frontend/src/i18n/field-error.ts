import { useTranslations } from "next-intl"
import type { FieldError } from "react-hook-form"

// 폼 칸 오류 문구. Zod 스키마는 문구 대신 messages/ko.json 키를 적는다 (lib/schemas)
// 서버 V001처럼 칸만 오류로 표시할 때는 message를 비워 둔다 → 빈 배열 (FieldError가 문구를 그리지 않는다)
export function useFieldErrorText() {
  const t = useTranslations()
  type Key = Parameters<typeof t>[0]
  return (error: FieldError | undefined): { message?: string }[] => {
    const key = error?.message?.trim()
    if (!key) return []
    return [{ message: t.has(key as Key) ? t(key as Key) : key }]
  }
}
