"use client"

import { useTranslations } from "next-intl"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { normalizeLink } from "@/lib/rich-text"

// 편집기의 링크 넣기 창. http·https·mailto만 받는다 (스킴이 없으면 https://를 붙인다)
export function LinkDialog({
  open,
  onOpenChange,
  initialHref,
  onApply,
  onRemove,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialHref: string
  onApply: (href: string) => void
  onRemove?: () => void
}) {
  const t = useTranslations("richText")
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("linkTitle")}</DialogTitle>
          <DialogDescription>{t("linkDescription")}</DialogDescription>
        </DialogHeader>
        {/* 열 때마다 새로 그려 칸을 지금 링크로 채운다 */}
        {open && (
          <LinkForm
            initialHref={initialHref}
            onApply={onApply}
            onRemove={onRemove}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function LinkForm({
  initialHref,
  onApply,
  onRemove,
  onCancel,
}: {
  initialHref: string
  onApply: (href: string) => void
  onRemove?: () => void
  onCancel: () => void
}) {
  const t = useTranslations("richText")
  const [value, setValue] = useState(initialHref)
  const [invalid, setInvalid] = useState(false)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    // 창은 body에 그려지지만 React 이벤트는 편집기를 감싼 폼까지 올라간다. 바깥 폼이 제출되지 않게 막는다
    event.stopPropagation()
    const href = normalizeLink(value)
    if (!href) {
      setInvalid(true)
      return
    }
    onApply(href)
  }

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <Field data-invalid={invalid}>
        <FieldLabel htmlFor="rich-text-link">{t("linkUrl")}</FieldLabel>
        <Input
          id="rich-text-link"
          type="url"
          inputMode="url"
          autoComplete="off"
          value={value}
          onChange={(event) => {
            setValue(event.target.value)
            setInvalid(false)
          }}
          aria-invalid={invalid}
          aria-describedby={invalid ? "rich-text-link-error" : undefined}
        />
        {invalid && <FieldError id="rich-text-link-error" errors={[{ message: t("linkInvalid") }]} />}
      </Field>
      <DialogFooter>
        {onRemove && (
          <Button type="button" variant="outline" onClick={onRemove}>
            {t("linkRemove")}
          </Button>
        )}
        <Button type="button" variant="outline" onClick={onCancel}>
          {t("cancel")}
        </Button>
        <Button type="submit">{t("linkApply")}</Button>
      </DialogFooter>
    </form>
  )
}
