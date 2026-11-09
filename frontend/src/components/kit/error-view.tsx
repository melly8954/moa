"use client"

import { Button } from "@/components/ui/button"

// 화면을 그리다 오류가 났을 때 보이는 모양 (app/error.tsx, app/global-error.tsx가 같이 쓴다)
// 문구는 부르는 쪽이 넘긴다: global-error는 문구 공급자 바깥이라 useTranslations를 쓰지 못한다
export function ErrorView({
  title,
  description,
  traceIdText,
  retryLabel,
  onRetry,
}: {
  title: string
  description: string
  // 문의 번호 문구 (예: "문의 번호: 3f2a..."). 서버 오류(S001)일 때만
  traceIdText?: string
  retryLabel: string
  onRetry: () => void
}) {
  return (
    <div role="alert" className="flex flex-1 flex-col items-center justify-center gap-4 p-(--page-padding) text-center">
      <h1 className="text-xl font-semibold">{title}</h1>
      <p className="text-sm text-muted-foreground">{description}</p>
      {traceIdText && <p className="font-mono text-xs text-muted-foreground">{traceIdText}</p>}
      <Button onClick={onRetry}>{retryLabel}</Button>
    </div>
  )
}
