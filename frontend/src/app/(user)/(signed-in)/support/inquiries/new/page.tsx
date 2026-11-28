"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { ImageUp } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useRef, useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { PageHeader } from "@/components/kit/page-header"
import { useUnsavedChanges } from "@/components/kit/use-unsaved-changes"
import { ImageThumbnails } from "@/components/support/image-thumbnails"
import { useSupportCategories } from "@/components/support/use-support-categories"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Textarea } from "@/components/ui/textarea"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { imageFileProblem, RICH_TEXT_IMAGE_TYPES } from "@/lib/rich-text"
import {
  INQUIRY_CONTENT_MAX,
  INQUIRY_IMAGE_MAX,
  INQUIRY_TITLE_MAX,
  type InquiryInput,
  inquirySchema,
} from "@/lib/schemas/inquiry"

// 1:1 문의 쓰기 (specs/017-customer-center US3, FR-007, contracts/screens.md "문의 쓰기"). 로그인 필요
//   이미지는 rich-text 이미지 올리기로 한 장씩 올려 받은 주소를 붙인다 (research R1). 다섯 장이면 붙이기를 끈다
//   보내면 상세로 간다. 칸 오류(V001)는 그 칸 아래에
export default function NewInquiryPage() {
  const t = useTranslations("inquiry")
  const router = useRouter()
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const categories = useSupportCategories()
  const fileInput = useRef<HTMLInputElement>(null)
  const [formError, setFormError] = useState<string>()
  const [fileError, setFileError] = useState<string>()
  const form = useForm<InquiryInput>({
    resolver: zodResolver(inquirySchema),
    defaultValues: { categoryCode: "", title: "", content: "", imageUrls: [] },
  })
  const imageUrls = useWatch({ control: form.control, name: "imageUrls" })
  const content = useWatch({ control: form.control, name: "content" })

  // 여러 장을 한 번에 고르면 함께 올라간다. 다섯 장을 넘지 않게 올리는 중인 수를 함께 센다
  const inFlight = useRef(0)
  const [uploading, setUploading] = useState(0)
  const full = imageUrls.length + uploading >= INQUIRY_IMAGE_MAX
  const upload = useMutation({
    mutationFn: (file: File) => api.v1.richText.uploadImage(file),
    onSuccess: (image) =>
      form.setValue("imageUrls", [...form.getValues("imageUrls"), image.url], { shouldDirty: true }),
    onError: (error) => setFileError(describeError(toApiError(error)).message),
    onSettled: () => {
      inFlight.current -= 1
      setUploading(inFlight.current)
    },
  })

  const save = useMutation({
    mutationFn: (input: InquiryInput) =>
      api.v1.inquiry.create({
        categoryCode: input.categoryCode,
        title: input.title.trim(),
        content: input.content,
        imageUrls: input.imageUrls,
      }),
    onSuccess: async (inquiry) => {
      form.reset(form.getValues())
      await queryClient.invalidateQueries({ queryKey: ["inquiries", "me"] })
      toast.success(t("created"))
      router.push(`/support/inquiries/${inquiry.id}`)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors) {
        form.setError(fieldError.field as keyof InquiryInput, { message: fieldError.message })
      }
      setFormError(describeError(apiError).message)
    },
  })
  useUnsavedChanges(form.formState.isDirty && !save.isSuccess)

  const choose = (files: FileList | null) => {
    setFileError(undefined)
    for (const file of Array.from(files ?? [])) {
      if (form.getValues("imageUrls").length + inFlight.current >= INQUIRY_IMAGE_MAX) {
        setFileError(t("imageMax"))
        return
      }
      const problem = imageFileProblem(file)
      if (problem) {
        setFileError(t(problem))
        return
      }
      inFlight.current += 1
      setUploading(inFlight.current)
      upload.mutate(file)
    }
  }

  return (
    <>
      <PageHeader title={t("newTitle")} description={t("newDescription")} />
      <form
        noValidate
        className="max-w-2xl"
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
                <FieldLabel htmlFor="inquiry-category">{t("fieldCategory")}</FieldLabel>
                <NativeSelect
                  {...field}
                  id="inquiry-category"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "inquiry-category-error" : undefined}
                >
                  <NativeSelectOption value="">{t("categoryChoose")}</NativeSelectOption>
                  {(categories.data ?? []).map((c) => (
                    <NativeSelectOption key={c.code} value={c.code}>
                      {c.name}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
                {fieldState.invalid && (
                  <FieldError id="inquiry-category-error" errors={fieldErrorText(fieldState.error)} />
                )}
              </Field>
            )}
          />
          <Controller
            name="title"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="inquiry-title">{t("fieldTitle")}</FieldLabel>
                <Input
                  {...field}
                  id="inquiry-title"
                  maxLength={INQUIRY_TITLE_MAX}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "inquiry-title-error" : undefined}
                />
                {fieldState.invalid && (
                  <FieldError id="inquiry-title-error" errors={fieldErrorText(fieldState.error)} />
                )}
              </Field>
            )}
          />
          <Controller
            name="content"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="inquiry-content">{t("fieldContent")}</FieldLabel>
                <Textarea
                  {...field}
                  id="inquiry-content"
                  rows={8}
                  maxLength={INQUIRY_CONTENT_MAX}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "inquiry-content-error" : "inquiry-content-count"}
                />
                {fieldState.invalid ? (
                  <FieldError id="inquiry-content-error" errors={fieldErrorText(fieldState.error)} />
                ) : (
                  <FieldDescription id="inquiry-content-count" className="text-right tabular-nums">
                    {t("contentCount", { count: content.length, max: INQUIRY_CONTENT_MAX })}
                  </FieldDescription>
                )}
              </Field>
            )}
          />
          <Field data-invalid={!!fileError || !!form.formState.errors.imageUrls}>
            <FieldLabel htmlFor="inquiry-images">{t("fieldImages")}</FieldLabel>
            <div className="flex flex-wrap items-center gap-2">
              <input
                ref={fileInput}
                id="inquiry-images"
                type="file"
                multiple
                accept={RICH_TEXT_IMAGE_TYPES.join(",")}
                className="sr-only"
                aria-describedby="inquiry-images-hint"
                disabled={full}
                onChange={(e) => {
                  choose(e.target.files)
                  e.target.value = ""
                }}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={full}
                onClick={() => fileInput.current?.click()}
              >
                <ImageUp />
                {t("imageAttach")}
              </Button>
              {uploading > 0 && <span className="text-sm text-muted-foreground">{t("imageUploading")}</span>}
            </div>
            <FieldDescription id="inquiry-images-hint">
              {full ? t("imageMax") : t("imageHint", { count: imageUrls.length, max: INQUIRY_IMAGE_MAX })}
            </FieldDescription>
            <ImageThumbnails
              urls={imageUrls}
              onRemove={(index) =>
                form.setValue(
                  "imageUrls",
                  imageUrls.filter((_, i) => i !== index),
                  { shouldDirty: true },
                )
              }
            />
            {(fileError || form.formState.errors.imageUrls) && (
              <FieldError
                errors={
                  fileError
                    ? [{ message: fileError }]
                    : fieldErrorText({ type: "manual", message: form.formState.errors.imageUrls?.message })
                }
              />
            )}
          </Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => router.push("/support")}>
              {t("cancel")}
            </Button>
            <Button type="submit" disabled={save.isPending || uploading > 0}>
              {t("submit")}
            </Button>
          </div>
        </FieldGroup>
      </form>
    </>
  )
}
