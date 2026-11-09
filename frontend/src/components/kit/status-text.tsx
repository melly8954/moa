import { cn } from "@/lib/utils"

// 상태 보이기: 점 + 글자 (색만으로 뜻을 전하지 않는다)
//   <StatusText tone="positive">사용</StatusText>
// tone은 뜻만 정한다. 색은 테마 변수에서 온다 (상태 색 규칙은 디자인 때 정한다. 지금은 임시)
//   positive: 정상·사용·완료 / negative: 실패·중지·오류 / neutral: 대기·사용 안 함 등 나머지
export type StatusTone = "positive" | "negative" | "neutral"

const DOT: Record<StatusTone, string> = {
  positive: "bg-primary",
  negative: "bg-destructive",
  neutral: "bg-muted-foreground",
}

export function StatusText({
  tone = "neutral",
  children,
  className,
}: {
  tone?: StatusTone
  children: React.ReactNode
  className?: string
}) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 whitespace-nowrap", className)}>
      <span aria-hidden="true" className={cn("size-2 shrink-0 rounded-full", DOT[tone])} />
      {children}
    </span>
  )
}
