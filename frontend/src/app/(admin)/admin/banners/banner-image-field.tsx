"use client"

import { useMutation } from "@tanstack/react-query"
import { ImageUp, X } from "lucide-react"
import { useTranslations } from "next-intl"
import { useId, useRef, useState } from "react"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"
import { useErrorMessages } from "@/lib/error-messages"
import { RICH_TEXT_IMAGE_MAX_SIZE } from "@/lib/rich-text"

// 배너 이미지 칸 하나 (specs/016-banner-popup FR-003, research R1·R2)
//   파일을 고르면 형식(JPG·PNG·WebP)과 크기(5MB)를 먼저 보고, rich-text 이미지 올리기로 올려 받은 주소를 값으로 둔다
//   서버도 같은 규칙으로 다시 본다(GIF는 배너 저장 때, 크기는 올리기 때 V001)
export const BANNER_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"]

export function BannerImageField({
  label,
  hint,
  value,
  onChange,
  required,
  invalid,
  errors,
}: {
  label: string
  hint: string
  value: string
  onChange: (url: string) => void
  required?: boolean
  invalid?: boolean
  errors: { message?: string }[]
}) {
  const t = useTranslations("banner")
  const { describeError } = useErrorMessages()
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const [fileError, setFileError] = useState<string>()
  const upload = useMutation({
    mutationFn: (file: File) => api.v1.richText.uploadImage(file),
    onSuccess: (image) => onChange(image.url),
    onError: (error) => setFileError(describeError(toApiError(error)).message),
  })

  const choose = (file: File | undefined) => {
    if (!file) return
    setFileError(undefined)
    if (!BANNER_IMAGE_TYPES.includes(file.type)) return setFileError(t("imageType"))
    if (file.size > RICH_TEXT_IMAGE_MAX_SIZE) return setFileError(t("imageTooLarge"))
    upload.mutate(file)
  }

  const shownErrors = fileError ? [{ message: fileError }] : errors
  return (
    <Field data-invalid={invalid || !!fileError}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={input}
          id={id}
          type="file"
          accept={BANNER_IMAGE_TYPES.join(",")}
          className="sr-only"
          aria-describedby={`${id}-hint`}
          aria-invalid={invalid || !!fileError || undefined}
          onChange={(e) => {
            choose(e.target.files?.[0])
            e.target.value = ""
          }}
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={upload.isPending}
          onClick={() => input.current?.click()}
        >
          <ImageUp />
          {value ? t("imageReplace") : t("imageChoose")}
        </Button>
        {value && !required && (
          <Button type="button" variant="ghost" size="sm" onClick={() => onChange("")}>
            <X />
            {t("imageRemove")}
          </Button>
        )}
        {upload.isPending && <span className="text-sm text-muted-foreground">{t("imageUploading")}</span>}
      </div>
      <FieldDescription id={`${id}-hint`}>{hint}</FieldDescription>
      {(invalid || fileError) && <FieldError errors={shownErrors} />}
    </Field>
  )
}
