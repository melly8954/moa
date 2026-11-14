"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useTranslations } from "next-intl"
import { useEffect } from "react"
import { Controller, useForm } from "react-hook-form"
import { useUnsavedChanges } from "@/components/kit/use-unsaved-changes"
import { RichTextEditor } from "@/components/rich-text/rich-text-editor"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { type NoticeInput, noticeSchema } from "@/lib/schemas/notice"

// 저장했으면 true (폼을 저장한 값으로 되돌려 떠날 때 되묻지 않는다)
export type NoticeFormSubmit = (values: NoticeInput, intent: "save" | "publish") => Promise<boolean>

// 공지 쓰기·고치기 폼 (specs/006-notices). 본문은 키트 rich-text 편집기
//   actions: 폼 아래 버튼. 누른 버튼이 intent(save·publish)를 정한다 (name="intent")
//   showPinned: 고치기 화면은 고정을 따로 바꾸므로 숨긴다
export function NoticeForm({
  initial,
  showPinned,
  onSubmit,
  actions,
}: {
  initial?: NoticeInput
  showPinned: boolean
  onSubmit: NoticeFormSubmit
  actions: React.ReactNode
}) {
  const t = useTranslations("notice")
  const fieldErrorText = useFieldErrorText()
  const form = useForm<NoticeInput>({
    resolver: zodResolver(noticeSchema),
    defaultValues: initial ?? { title: "", content: "", pinned: false },
  })
  useUnsavedChanges(form.formState.isDirty)

  // 불러온 공지는 reset으로 넣는다 (편집기가 밖에서 바뀐 값을 받아 그린다, dirty가 되지 않는다)
  useEffect(() => {
    if (initial) form.reset(initial)
  }, [initial, form])

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    const submitter = (event.nativeEvent as SubmitEvent).submitter as HTMLButtonElement | null
    const intent = submitter?.value === "publish" ? "publish" : "save"
    return form.handleSubmit(async (values) => {
      if (await onSubmit(values, intent)) form.reset(values)
    })(event)
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex max-w-3xl min-w-0 flex-col gap-4">
      <FieldGroup>
        <Controller
          name="title"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="title">{t("fieldTitle")}</FieldLabel>
              <Input
                {...field}
                id="title"
                aria-invalid={fieldState.invalid}
                aria-describedby={fieldState.invalid ? "title-error" : undefined}
              />
              {fieldState.invalid && <FieldError id="title-error" errors={fieldErrorText(fieldState.error)} />}
            </Field>
          )}
        />
        <Controller
          name="content"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel id="content-label">{t("fieldContent")}</FieldLabel>
              <RichTextEditor
                {...field}
                id="content"
                aria-labelledby="content-label"
                aria-invalid={fieldState.invalid}
                aria-describedby={fieldState.invalid ? "content-error" : "content-help"}
              />
              {fieldState.invalid ? (
                <FieldError id="content-error" errors={fieldErrorText(fieldState.error)} />
              ) : (
                <FieldDescription id="content-help">{t("contentHelp")}</FieldDescription>
              )}
            </Field>
          )}
        />
        {showPinned && (
          <Controller
            name="pinned"
            control={form.control}
            render={({ field }) => (
              <Field orientation="horizontal">
                {/* Base UI 체크박스는 label만으로 이름이 붙지 않아 aria-label을 함께 준다 */}
                <Checkbox
                  id="pinned"
                  aria-label={t("pinnedLabel")}
                  checked={field.value}
                  onCheckedChange={(checked) => field.onChange(checked)}
                />
                <FieldLabel htmlFor="pinned">{t("pinnedLabel")}</FieldLabel>
              </Field>
            )}
          />
        )}
      </FieldGroup>
      <div className="flex flex-wrap justify-end gap-2">{actions}</div>
    </form>
  )
}
