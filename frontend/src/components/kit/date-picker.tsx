"use client"

import { CalendarDays } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from "@/components/ui/input-group"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { CALENDAR_LOCALES } from "@/i18n/calendar-locale"
import {
  type DateRange,
  type DateString,
  fromCalendarDate,
  inRange,
  parseTypedDate,
  QUICK_RANGES,
  quickRange,
  toCalendarDate,
} from "@/lib/date"
import { cn } from "@/lib/utils"

// 날짜 고르기: 입력칸에 직접 치거나(20261106, 2026.11.6도 된다) 달력 버튼으로 고른다
// 값은 언제나 "YYYY-MM-DD" 문자열이고 비우면 "" (lib/date.ts). min·max 밖은 고를 수 없다
// 입력칸 이름은 aria-label이나 <label htmlFor={id}>로 붙인다

type BaseProps = {
  id?: string
  min?: DateString
  max?: DateString
  disabled?: boolean
  invalid?: boolean
  className?: string
}

function disabledDays(min?: DateString, max?: DateString) {
  const before = min ? toCalendarDate(min) : undefined
  const after = max ? toCalendarDate(max) : undefined
  return [...(before ? [{ before }] : []), ...(after ? [{ after }] : [])]
}

// 입력칸 하나. 칸을 떠날 때 읽어서 맞으면 onCommit, 아니면 빨간 테두리로 남긴다
function DateTextInput({
  value,
  onCommit,
  min,
  max,
  invalid,
  ...props
}: {
  value: DateString
  onCommit: (value: DateString) => void
  min?: DateString
  max?: DateString
  invalid?: boolean
} & Omit<React.ComponentProps<typeof InputGroupInput>, "value" | "onChange">) {
  const [text, setText] = useState(value)
  const [typedWrong, setTypedWrong] = useState(false)
  const [lastValue, setLastValue] = useState(value)
  // 바깥 값이 바뀌면(달력·빠른 선택·초기화) 입력칸 글자도 바꾼다
  if (value !== lastValue) {
    setLastValue(value)
    setText(value)
    setTypedWrong(false)
  }

  const commit = () => {
    const parsed = parseTypedDate(text)
    if (parsed === undefined || (parsed !== "" && !inRange(parsed, min, max))) {
      setTypedWrong(true)
      return
    }
    setTypedWrong(false)
    setText(parsed)
    if (parsed !== value) onCommit(parsed)
  }

  return (
    <InputGroupInput
      {...props}
      value={text}
      inputMode="numeric"
      placeholder="YYYY-MM-DD"
      aria-invalid={invalid || typedWrong || undefined}
      onChange={(e) => setText(e.target.value)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === "Enter") commit()
      }}
    />
  )
}

export function DatePicker({
  value,
  onChange,
  min,
  max,
  disabled,
  invalid,
  className,
  ...inputProps
}: BaseProps & {
  value: DateString
  onChange: (value: DateString) => void
  "aria-label"?: string
}) {
  const t = useTranslations("datePicker")
  const locale = CALENDAR_LOCALES[useLocale()]
  const [open, setOpen] = useState(false)
  return (
    <InputGroup className={cn("w-(--date-input-width)", className)} data-disabled={disabled || undefined}>
      <DateTextInput
        {...inputProps}
        value={value}
        onCommit={onChange}
        min={min}
        max={max}
        invalid={invalid}
        disabled={disabled}
      />
      <InputGroupAddon align="inline-end">
        <Popover open={open} onOpenChange={setOpen}>
          <PopoverTrigger render={<InputGroupButton size="icon-xs" aria-label={t("open")} disabled={disabled} />}>
            <CalendarDays />
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="end">
            <Calendar
              mode="single"
              locale={locale}
              selected={toCalendarDate(value)}
              defaultMonth={toCalendarDate(value)}
              disabled={disabledDays(min, max)}
              onSelect={(date) => {
                onChange(fromCalendarDate(date))
                setOpen(false)
              }}
            />
          </PopoverContent>
        </Popover>
      </InputGroupAddon>
    </InputGroup>
  )
}

// 기간 고르기: 시작·끝 입력칸 둘과 달력 하나, 빠른 선택(오늘, 최근 7일, 최근 30일, 이번 달, 지난달)
// 시작이 끝보다 늦으면 빨간 테두리로 보인다 (값은 그대로 넘긴다. 검사는 폼·검색이 한다)
export function DateRangePicker({
  value,
  onChange,
  min,
  max,
  disabled,
  invalid,
  className,
  fromLabel,
  toLabel,
}: BaseProps & {
  value: DateRange
  onChange: (value: DateRange) => void
  // 입력칸 이름 (화면 낭독기). 기본은 "시작일"·"종료일"
  fromLabel?: string
  toLabel?: string
}) {
  const t = useTranslations("datePicker")
  const locale = CALENDAR_LOCALES[useLocale()]
  const [open, setOpen] = useState(false)
  const reversed = value.from !== "" && value.to !== "" && value.from > value.to
  const from = toCalendarDate(value.from)
  const to = toCalendarDate(value.to)

  return (
    <div className={cn("flex flex-wrap items-center gap-1", className)}>
      {/* 시작 ~ 끝은 한 줄로 붙여 둔다. 좁으면 달력 버튼만 다음 줄로 */}
      <div className="flex items-center gap-1">
        <InputGroup className="w-(--date-range-input-width)" data-disabled={disabled || undefined}>
          <DateTextInput
            aria-label={fromLabel ?? t("from")}
            value={value.from}
            onCommit={(next) => onChange({ ...value, from: next })}
            min={min}
            max={max}
            invalid={invalid || reversed}
            disabled={disabled}
          />
        </InputGroup>
        <span aria-hidden="true" className="text-muted-foreground">
          ~
        </span>
        <InputGroup className="w-(--date-range-input-width)" data-disabled={disabled || undefined}>
          <DateTextInput
            aria-label={toLabel ?? t("to")}
            value={value.to}
            onCommit={(next) => onChange({ ...value, to: next })}
            min={min}
            max={max}
            invalid={invalid || reversed}
            disabled={disabled}
          />
        </InputGroup>
      </div>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          render={<Button variant="outline" size="icon" aria-label={t("openRange")} disabled={disabled} />}
        >
          <CalendarDays />
        </PopoverTrigger>
        <PopoverContent className="flex w-auto flex-col gap-2 p-2 sm:flex-row" align="end">
          <div className="flex flex-wrap gap-1 sm:flex-col">
            {QUICK_RANGES.map((kind) => (
              <Button
                key={kind}
                variant="ghost"
                size="sm"
                className="justify-start"
                onClick={() => {
                  onChange(quickRange(kind))
                  setOpen(false)
                }}
              >
                {t(kind)}
              </Button>
            ))}
          </div>
          <Calendar
            mode="range"
            locale={locale}
            selected={{ from, to }}
            defaultMonth={from ?? to}
            disabled={disabledDays(min, max)}
            onSelect={(range) => onChange({ from: fromCalendarDate(range?.from), to: fromCalendarDate(range?.to) })}
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}
