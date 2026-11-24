"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { type Control, Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { ExposureFields } from "@/components/marketing/exposure-fields"
import { RichTextEditor } from "@/components/rich-text/rich-text-editor"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { fromOffsetDateTime } from "@/lib/marketing"
import { EXPOSURE_DEFAULTS, type ExposureInput, toExposureRequest } from "@/lib/schemas/marketing"
import { type PopupInput, popupSchema } from "@/lib/schemas/popup"
import type { AdminPopup } from "@/types/popup"

// 팝업 드로어 (specs/016-banner-popup FR-004·FR-005, contracts/admin-screens.md "팝업 드로어")
//   내용은 공지와 같은 rich-text 편집기라 넓은 드로어를 쓴다 (research R9)
//   popup이 있으면 고치기(아래 줄 삭제·저장), 없으면 등록(취소·등록). 순서 기본 0, 켜짐 기본 켜짐
function toInput(popup?: AdminPopup): PopupInput {
  if (!popup) return { title: "", content: "", ...EXPOSURE_DEFAULTS }
  const start = fromOffsetDateTime(popup.startsAt)
  const end = fromOffsetDateTime(popup.endsAt)
  return {
    title: popup.title,
    content: popup.content,
    linkUrl: popup.linkUrl ?? "",
    startDate: start.date,
    startTime: start.time,
    endDate: end.date,
    endTime: end.time,
    sortOrder: String(popup.sortOrder),
    enabled: popup.enabled,
  }
}

const FIELD_OF: Record<string, keyof PopupInput> = { startsAt: "startDate", endsAt: "endDate" }

export function PopupDrawer({
  popup,
  open,
  onOpenChange,
  onSaved,
  onRemove,
}: {
  popup?: AdminPopup
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
  onRemove?: () => void
}) {
  const t = useTranslations("popup")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!popup
  const [formError, setFormError] = useState<string>()
  const form = useForm<PopupInput>({ resolver: zodResolver(popupSchema), defaultValues: toInput(popup) })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)

  const save = useMutation({
    mutationFn: (input: PopupInput) => {
      const data = { title: input.title.trim(), content: input.content, ...toExposureRequest(input) }
      return editing ? api.v1.popupAdmin.modify(popup.id, data) : api.v1.popupAdmin.create(data)
    },
    onSuccess: async () => {
      await onSaved()
      toast.success(t(editing ? "modified" : "created"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors) {
        const name = FIELD_OF[fieldError.field] ?? (fieldError.field as keyof PopupInput)
        form.setError(name, { message: fieldError.message })
      }
      setFormError(describeError(apiError).message)
    },
  })

  return (
    <SideDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={t(editing ? "editTitle" : "createTitle")}
      description={t("drawerDescription")}
      dirty={form.formState.isDirty}
      className="data-[side=right]:md:max-w-3xl"
      footer={
        <>
          {editing && onRemove && (
            <Button type="button" variant="outline" className="mr-auto text-destructive" onClick={onRemove}>
              {t("remove")}
            </Button>
          )}
          <Button type="button" variant="outline" onClick={() => void close()}>
            {t("cancel")}
          </Button>
          <Button type="submit" form="popup-form" disabled={save.isPending}>
            {t(editing ? "save" : "create")}
          </Button>
        </>
      }
    >
      <form
        id="popup-form"
        noValidate
        onSubmit={form.handleSubmit((input) => {
          setFormError(undefined)
          save.mutate(input)
        })}
      >
        <FieldGroup>
          {formError && (
            <Alert variant="destructive">
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <Controller
            name="title"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="popup-title">{t("fieldTitle")}</FieldLabel>
                <Input
                  {...field}
                  id="popup-title"
                  maxLength={200}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "popup-title-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="popup-title-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="content"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel id="popup-content-label">{t("fieldContent")}</FieldLabel>
                <RichTextEditor
                  {...field}
                  id="popup-content"
                  aria-labelledby="popup-content-label"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "popup-content-error" : "popup-content-help"}
                />
                {fieldState.invalid ? (
                  <FieldError id="popup-content-error" errors={fieldErrorText(fieldState.error)} />
                ) : (
                  <FieldDescription id="popup-content-help">{t("contentHelp")}</FieldDescription>
                )}
              </Field>
            )}
          />
          {/* 팝업 폼은 ExposureInput 칸을 모두 가진다 */}
          <ExposureFields control={form.control as unknown as Control<ExposureInput>} idPrefix="popup" />
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
