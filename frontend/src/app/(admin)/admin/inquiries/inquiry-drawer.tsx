"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { RichTextEditor } from "@/components/rich-text/rich-text-editor"
import { ImageThumbnails } from "@/components/support/image-thumbnails"
import { InquiryStatusText } from "@/components/support/inquiry-status"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import { type AnswerInput, answerSchema } from "@/lib/schemas/inquiry"
import type { AdminInquiry } from "@/types/inquiry"
import { authorText } from "./author"

// 문의 드로어 (specs/017-customer-center US4, contracts/screens.md "문의 관리")
//   위에 회원이 쓴 문의(내용은 줄바꿈 그대로, 첨부는 누르면 크게), 아래에 답변 편집기와 저장
//   답변 완료면 답변 시각과 답변한 관리자를 보이고 저장은 "답변 고치기"다 (고쳐도 알림은 다시 가지 않는다, FR-008)
//   그 사이 회원이 지운 문의(R001)면 안내하고 목록을 다시 읽는다 (spec Edge Cases)
export function InquiryDrawer({
  inquiry,
  open,
  onOpenChange,
  onSaved,
  onMissing,
}: {
  inquiry: AdminInquiry
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
  onMissing: () => void
}) {
  const t = useTranslations("inquiry")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const answered = inquiry.status === "ANSWERED"
  const [formError, setFormError] = useState<string>()
  const form = useForm<AnswerInput>({
    resolver: zodResolver(answerSchema),
    defaultValues: { content: inquiry.answer ?? "" },
  })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)

  const save = useMutation({
    mutationFn: (input: AnswerInput) => api.v1.inquiryAdmin.answer(inquiry.id, input.content),
    onSuccess: async () => {
      await onSaved()
      toast.success(t(answered ? "answerModified" : "answered"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R001") {
        toast.error(t("notFound"))
        onMissing()
        return
      }
      for (const fieldError of apiError.fieldErrors) {
        form.setError("content", { message: fieldError.message })
      }
      setFormError(describeError(apiError).message)
    },
  })

  return (
    <SideDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={t("drawerTitle")}
      description={t("drawerDescription")}
      dirty={form.formState.isDirty}
      className="data-[side=right]:md:max-w-3xl"
      footer={
        <>
          <Button type="button" variant="outline" onClick={() => void close()}>
            {t("cancel")}
          </Button>
          {inquiry.answerable && (
            <Button type="submit" form="inquiry-answer-form" disabled={save.isPending}>
              {t(answered ? "answerModify" : "answerSave")}
            </Button>
          )}
        </>
      }
    >
      <div className="flex flex-col gap-6">
        <section className="flex flex-col gap-3" aria-labelledby="inquiry-question-title">
          <div className="flex flex-col gap-1">
            <p className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              <InquiryStatusText status={inquiry.status} />
              <span>{inquiry.categoryName}</span>
            </p>
            <h3 id="inquiry-question-title" className="text-base font-semibold break-words">
              {inquiry.title}
            </h3>
            <p className="text-sm text-muted-foreground">
              {authorText(inquiry.author, t("authorWithdrawn"))} ·{" "}
              <time dateTime={inquiry.createdAt}>{formatDateTime(inquiry.createdAt)}</time>
            </p>
          </div>
          <p className="text-sm break-words whitespace-pre-wrap">{inquiry.content}</p>
          <ImageThumbnails urls={inquiry.imageUrls} />
        </section>
        {inquiry.answerable ? (
          <form
            id="inquiry-answer-form"
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
                name="content"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel id="inquiry-answer-label">{t("fieldAnswer")}</FieldLabel>
                    <RichTextEditor
                      {...field}
                      id="inquiry-answer"
                      aria-labelledby="inquiry-answer-label"
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? "inquiry-answer-error" : "inquiry-answer-help"}
                    />
                    {fieldState.invalid ? (
                      <FieldError id="inquiry-answer-error" errors={fieldErrorText(fieldState.error)} />
                    ) : (
                      <FieldDescription id="inquiry-answer-help">
                        {answered && inquiry.answeredAt
                          ? t("answeredBy", {
                              name: inquiry.answeredByName ?? "-",
                              time: formatDateTime(inquiry.answeredAt),
                            })
                          : t("answerHelp")}
                      </FieldDescription>
                    )}
                  </Field>
                )}
              />
            </FieldGroup>
          </form>
        ) : (
          <Alert>
            <AlertDescription>{t("notAnswerable")}</AlertDescription>
          </Alert>
        )}
      </div>
    </SideDrawer>
  )
}
