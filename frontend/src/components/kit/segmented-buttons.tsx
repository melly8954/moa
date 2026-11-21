"use client"

import { cn } from "@/lib/utils"

// 나눔 단추: 하나를 고르는 단추 묶음 (moa, specs/011-list-screen FR-010, 2026-11-21. 키트로 옮길 것)
//   검색 영역의 기간 빠른 단추와 고르기 조건(전체·성공·실패 등)이 함께 쓴다. 날짜 칸·입력 칸(h-8)과 같은 높이·테두리
//   고른 단추는 채운 모양(primary), 단추마다 aria-pressed. value가 어느 단추와도 같지 않으면 아무것도 고르지 않은 모양
//   맨 button을 쓴다: base-ui Toggle은 빈 값("")을 자동 id로 바꿔, "전체"(빈 값)를 고르면 엉뚱한 값이 조건에 들어갔다
export function SegmentedButtons<V extends string>({
  items,
  value,
  onChange,
  disabled,
  className,
  "aria-label": ariaLabel,
  "aria-labelledby": ariaLabelledBy,
}: {
  items: readonly { value: V; label: string }[]
  value: V | undefined
  onChange: (value: V) => void
  disabled?: boolean
  className?: string
  "aria-label"?: string
  "aria-labelledby"?: string
}) {
  return (
    <div
      role="group"
      aria-label={ariaLabel}
      aria-labelledby={ariaLabelledBy}
      className={cn("inline-flex h-8 items-center gap-0.5 rounded-lg border border-input p-0.5", className)}
    >
      {items.map((item) => {
        const pressed = item.value === value
        return (
          <button
            key={item.value || "all"}
            type="button"
            aria-pressed={pressed}
            disabled={disabled}
            className={cn(
              "h-full rounded-md px-2.5 text-sm whitespace-nowrap text-muted-foreground transition-colors outline-none",
              "hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50",
              pressed &&
                "bg-primary font-medium text-primary-foreground hover:bg-primary hover:text-primary-foreground",
            )}
            onClick={() => onChange(item.value)}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
