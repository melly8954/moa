"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { EnabledSwitch } from "@/components/kit/enabled-switch"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { RichTextEditor } from "@/components/rich-text/rich-text-editor"
import { useSupportCategories } from "@/components/support/use-support-categories"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type FaqInput, faqSchema } from "@/lib/schemas/faq"
import type { AdminFaq } from "@/types/faq"

// FAQ 드로어 (specs/017-customer-center FR-004, contracts/screens.md "FAQ 관리")
//   답은 공지와 같은 rich-text 편집기라 넓은 드로어를 쓴다 (research R12)
//   faq가 있으면 고치기(아래 줄 삭제·저장), 없으면 등록(취소·등록). 순서 기본 0, 켜짐 기본 켜짐
//   분류는 지금 고를 수 있는 항목만 보인다. 고칠 때 꺼진 분류면 그 항목을 맨 앞에 남겨 그대로 저장할 수 있다 (research R8)
function toInput(faq?: AdminFaq): FaqInput {
  if (!faq) return { categoryCode: "", question: "", answer: "", sortOrder: "0", enabled: true }
  return {
    categoryCode: faq.categoryCode,
    question: faq.question,
    answer: faq.answer,
    sortOrder: String(faq.sortOrder),
    enabled: faq.enabled,
  }
}

export function FaqDrawer({
  faq,
  open,
  onOpenChange,
  onSaved,
  onRemove,
}: {
  faq?: AdminFaq
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
  onRemove?: () => void
}) {
  const t = useTranslations("faq")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const categories = useSupportCategories()
  const editing = !!faq
  const [formError, setFormError] = useState<string>()
  const form = useForm<FaqInput>({ resolver: zodResolver(faqSchema), defaultValues: toInput(faq) })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)

  const options = categories.data ?? []
  const keepsInactive = faq && !options.some((c) => c.code === faq.categoryCode)

  const save = useMutation({
    mutationFn: (input: FaqInput) => {
      const data = {
        categoryCode: input.categoryCode,
        question: input.question.trim(),
        answer: input.answer,
        sortOrder: Number(input.sortOrder),
        enabled: input.enabled,
      }
      return editing ? api.v1.faqAdmin.modify(faq.id, data) : api.v1.faqAdmin.create(data)
    },
    onSuccess: async () => {
      await onSaved()
      toast.success(t(editing ? "modified" : "created"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors) {
        form.setError(fieldError.field as keyof FaqInput, { message: fieldError.message })
      }
      setFormError(apiError.code === "B301" ? t("limitReached") : describeError(apiError).message)
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
          <Button type="submit" form="faq-form" disabled={save.isPending}>
            {t(editing ? "save" : "register")}
          </Button>
        </>
      }
    >
      <form
        id="faq-form"
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
            name="categoryCode"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="faq-category">{t("fieldCategory")}</FieldLabel>
                <NativeSelect
                  {...field}
                  id="faq-category"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "faq-category-error" : undefined}
                >
                  <NativeSelectOption value="">{t("categoryChoose")}</NativeSelectOption>
                  {keepsInactive && (
                    <NativeSelectOption value={faq.categoryCode}>
                      {t("categoryInactive", { name: faq.categoryName })}
                    </NativeSelectOption>
                  )}
                  {options.map((c) => (
                    <NativeSelectOption key={c.code} value={c.code}>
                      {c.name}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
                {fieldState.invalid && <FieldError id="faq-category-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="question"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="faq-question">{t("fieldQuestion")}</FieldLabel>
                <Input
                  {...field}
                  id="faq-question"
                  maxLength={200}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "faq-question-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="faq-question-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="answer"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel id="faq-answer-label">{t("fieldAnswer")}</FieldLabel>
                <RichTextEditor
                  {...field}
                  id="faq-answer"
                  aria-labelledby="faq-answer-label"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "faq-answer-error" : "faq-answer-help"}
                />
                {fieldState.invalid ? (
                  <FieldError id="faq-answer-error" errors={fieldErrorText(fieldState.error)} />
                ) : (
                  <FieldDescription id="faq-answer-help">{t("answerHelp")}</FieldDescription>
                )}
              </Field>
            )}
          />
          <Controller
            name="sortOrder"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="faq-sort-order">{t("sortOrder")}</FieldLabel>
                <Input
                  {...field}
                  id="faq-sort-order"
                  inputMode="numeric"
                  className="w-24"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "faq-sort-order-error" : "faq-sort-order-hint"}
                />
                {fieldState.invalid ? (
                  <FieldError id="faq-sort-order-error" errors={fieldErrorText(fieldState.error)} />
                ) : (
                  <FieldDescription id="faq-sort-order-hint">{t("sortOrderHint")}</FieldDescription>
                )}
              </Field>
            )}
          />
          <Controller
            name="enabled"
            control={form.control}
            render={({ field }) => (
              <EnabledSwitch
                id="faq-enabled"
                label={t("enabled")}
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
