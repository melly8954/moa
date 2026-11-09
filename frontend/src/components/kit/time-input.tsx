"use client"

import { useState } from "react"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

// 24시간 시각 칸 "HH:mm" (비우면 "")
//   브라우저 <input type="time">은 한국어 OS에서 "오후 02:30"으로 보여 쓰지 않는다
//   "930", "9:30", "0930"을 쳐도 칸을 떠날 때 "09:30"으로 맞춘다. 못 읽으면 빨간 테두리로 남긴다

export function parseTime(text: string): string | undefined {
  const trimmed = text.trim()
  if (trimmed === "") return ""
  const match = /^(\d{1,2}):?(\d{2})$/.exec(trimmed)
  if (!match) return undefined
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (hour > 23 || minute > 59) return undefined
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`
}

export function TimeInput({
  value,
  onChange,
  invalid,
  className,
  ...props
}: {
  value: string
  onChange: (value: string) => void
  invalid?: boolean
} & Omit<React.ComponentProps<typeof Input>, "value" | "onChange" | "type">) {
  const [text, setText] = useState(value)
  const [typedWrong, setTypedWrong] = useState(false)
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    setLastValue(value)
    setText(value)
    setTypedWrong(false)
  }

  const commit = () => {
    const parsed = parseTime(text)
    if (parsed === undefined) {
      setTypedWrong(true)
      return
    }
    setTypedWrong(false)
    setText(parsed)
    if (parsed !== value) onChange(parsed)
  }

  return (
    <Input
      {...props}
      className={cn("w-(--time-input-width)", className)}
      value={text}
      inputMode="numeric"
      placeholder="HH:mm"
      aria-invalid={invalid || typedWrong || undefined}
      onChange={(e) => setText(e.target.value)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === "Enter") commit()
      }}
    />
  )
}
