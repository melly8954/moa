"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { type Control, Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { ExposureFields } from "@/components/marketing/exposure-fields"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { fromOffsetDateTime } from "@/lib/marketing"
import { type BannerInput, bannerSchema } from "@/lib/schemas/banner"
import { EXPOSURE_DEFAULTS, type ExposureInput, toExposureRequest } from "@/lib/schemas/marketing"
import type { AdminBanner } from "@/types/banner"
import { BannerImageField } from "./banner-image-field"
import { BannerPreview } from "./banner-preview"

// 배너 드로어 (specs/016-banner-popup FR-003·FR-005, contracts/admin-screens.md "배너 드로어")
//   banner가 있으면 고치기(아래 줄 삭제·저장), 없으면 등록(취소·등록). 순서 기본 0, 켜짐 기본 켜짐 (US1-AC14)
//   저장 실패의 칸 오류(V001)는 서버 문구 그대로 그 칸 아래에 보인다 (US1-AC7)
function toInput(banner?: AdminBanner): BannerInput {
  if (!banner) return { name: "", wideImageUrl: "", narrowImageUrl: "", altText: "", ...EXPOSURE_DEFAULTS }
  const start = fromOffsetDateTime(banner.startsAt)
  const end = fromOffsetDateTime(banner.endsAt)
  return {
    name: banner.name,
    wideImageUrl: banner.wideImageUrl,
    narrowImageUrl: banner.narrowImageUrl ?? "",
    altText: banner.altText,
    linkUrl: banner.linkUrl ?? "",
    startDate: start.date,
    startTime: start.time,
    endDate: end.date,
    endTime: end.time,
    sortOrder: String(banner.sortOrder),
    enabled: banner.enabled,
  }
}

// 서버 칸 이름 → 폼 칸 이름 (일시는 날짜 칸에 붙인다)
const FIELD_OF: Record<string, keyof BannerInput> = { startsAt: "startDate", endsAt: "endDate" }

export function BannerDrawer({
  banner,
  open,
  onOpenChange,
  onSaved,
  onRemove,
}: {
  banner?: AdminBanner
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
  onRemove?: () => void
}) {
  const t = useTranslations("banner")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!banner
  const [formError, setFormError] = useState<string>()
  const form = useForm<BannerInput>({ resolver: zodResolver(bannerSchema), defaultValues: toInput(banner) })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)
  const [wideImageUrl, narrowImageUrl, altText] = useWatch({
    control: form.control,
    name: ["wideImageUrl", "narrowImageUrl", "altText"],
  })

  const save = useMutation({
    mutationFn: (input: BannerInput) => {
      const data = {
        name: input.name.trim(),
        wideImageUrl: input.wideImageUrl,
        narrowImageUrl: input.narrowImageUrl || undefined,
        altText: input.altText.trim(),
        ...toExposureRequest(input),
      }
      return editing ? api.v1.bannerAdmin.modify(banner.id, data) : api.v1.bannerAdmin.create(data)
    },
    onSuccess: async () => {
      await onSaved()
      toast.success(t(editing ? "modified" : "created"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors) {
        const name = FIELD_OF[fieldError.field] ?? (fieldError.field as keyof BannerInput)
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
          <Button type="submit" form="banner-form" disabled={save.isPending}>
            {t(editing ? "save" : "register")}
          </Button>
        </>
      }
    >
      <form
        id="banner-form"
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
            name="name"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="banner-name">{t("name")}</FieldLabel>
                <Input
                  {...field}
                  id="banner-name"
                  maxLength={100}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "banner-name-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="banner-name-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="wideImageUrl"
            control={form.control}
            render={({ field, fieldState }) => (
              <BannerImageField
                label={t("wideImage")}
                hint={t("wideImageHint")}
                value={field.value}
                onChange={(url) => field.onChange(url)}
                required
                invalid={fieldState.invalid}
                errors={fieldErrorText(fieldState.error)}
              />
            )}
          />
          <Controller
            name="narrowImageUrl"
            control={form.control}
            render={({ field, fieldState }) => (
              <BannerImageField
                label={t("narrowImage")}
                hint={t("narrowImageHint")}
                value={field.value}
                onChange={(url) => field.onChange(url)}
                invalid={fieldState.invalid}
                errors={fieldErrorText(fieldState.error)}
              />
            )}
          />
          <BannerPreview wideImageUrl={wideImageUrl} narrowImageUrl={narrowImageUrl} altText={altText} />
          <Controller
            name="altText"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="banner-alt-text">{t("altText")}</FieldLabel>
                <Input
                  {...field}
                  id="banner-alt-text"
                  maxLength={200}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "banner-alt-text-error" : "banner-alt-text-hint"}
                />
                {fieldState.invalid ? (
                  <FieldError id="banner-alt-text-error" errors={fieldErrorText(fieldState.error)} />
                ) : (
                  <p id="banner-alt-text-hint" className="text-sm text-muted-foreground">
                    {t("altTextHint")}
                  </p>
                )}
              </Field>
            )}
          />
          {/* 배너 폼은 ExposureInput 칸을 모두 가진다 */}
          <ExposureFields control={form.control as unknown as Control<ExposureInput>} idPrefix="banner" />
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
