"use client"

import { useTranslations } from "next-intl"
import { type Control, Controller } from "react-hook-form"
import { DatePicker } from "@/components/kit/date-picker"
import { EnabledSwitch } from "@/components/kit/enabled-switch"
import { TimeInput } from "@/components/kit/time-input"
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import type { ExposureInput } from "@/lib/schemas/marketing"

// 배너·팝업 드로어가 같이 쓰는 칸 (specs/016-banner-popup contracts/admin-screens.md)
//   연결 주소, 노출 기간(시작 날짜·시각 필수, 끝 날짜·시각 선택), 순서(기본 0), 켜짐(기본 켜짐)
//   control은 ExposureInput 칸을 가진 폼의 것을 넘긴다 (배너·팝업 폼은 이 칸을 모두 가진다)
export function ExposureFields({ control, idPrefix }: { control: Control<ExposureInput>; idPrefix: string }) {
  const t = useTranslations("marketing")
  const fieldErrorText = useFieldErrorText()
  const id = (name: string) => `${idPrefix}-${name}`

  return (
    <>
      <Controller
        name="linkUrl"
        control={control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("link-url")}>{t("linkUrl")}</FieldLabel>
            <Input
              {...field}
              id={id("link-url")}
              inputMode="url"
              placeholder="/notices/1"
              autoComplete="off"
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("link-url-error") : id("link-url-hint")}
            />
            {fieldState.invalid ? (
              <FieldError id={id("link-url-error")} errors={fieldErrorText(fieldState.error)} />
            ) : (
              <FieldDescription id={id("link-url-hint")}>{t("linkUrlHint")}</FieldDescription>
            )}
          </Field>
        )}
      />
      <Field>
        <FieldLabel id={id("start-label")}>{t("startsAt")}</FieldLabel>
        <div className="flex flex-wrap gap-2">
          <Controller
            name="startDate"
            control={control}
            render={({ field, fieldState }) => (
              <DatePicker
                value={field.value}
                onChange={field.onChange}
                invalid={fieldState.invalid}
                aria-label={t("startDate")}
              />
            )}
          />
          <Controller
            name="startTime"
            control={control}
            render={({ field, fieldState }) => (
              <TimeInput
                value={field.value}
                onChange={field.onChange}
                invalid={fieldState.invalid}
                aria-label={t("startTime")}
              />
            )}
          />
        </div>
        <Controller
          name="startDate"
          control={control}
          render={({ fieldState }) => (
            <>{fieldState.invalid && <FieldError errors={fieldErrorText(fieldState.error)} />}</>
          )}
        />
      </Field>
      <Field>
        <FieldLabel id={id("end-label")}>{t("endsAt")}</FieldLabel>
        <div className="flex flex-wrap gap-2">
          <Controller
            name="endDate"
            control={control}
            render={({ field, fieldState }) => (
              <DatePicker
                value={field.value}
                onChange={field.onChange}
                invalid={fieldState.invalid}
                aria-label={t("endDate")}
              />
            )}
          />
          <Controller
            name="endTime"
            control={control}
            render={({ field }) => (
              <TimeInput value={field.value} onChange={field.onChange} aria-label={t("endTime")} />
            )}
          />
        </div>
        <Controller
          name="endDate"
          control={control}
          render={({ fieldState }) =>
            fieldState.invalid ? (
              <FieldError id={id("end-error")} errors={fieldErrorText(fieldState.error)} />
            ) : (
              <FieldDescription>{t("endsAtHint")}</FieldDescription>
            )
          }
        />
      </Field>
      <Controller
        name="sortOrder"
        control={control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("sort-order")}>{t("sortOrder")}</FieldLabel>
            <Input
              {...field}
              id={id("sort-order")}
              inputMode="numeric"
              className="w-24"
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("sort-order-error") : id("sort-order-hint")}
            />
            {fieldState.invalid ? (
              <FieldError id={id("sort-order-error")} errors={fieldErrorText(fieldState.error)} />
            ) : (
              <FieldDescription id={id("sort-order-hint")}>{t("sortOrderHint")}</FieldDescription>
            )}
          </Field>
        )}
      />
      <Controller
        name="enabled"
        control={control}
        render={({ field }) => (
          <EnabledSwitch
            id={id("enabled")}
            label={t("enabled")}
            checked={field.value}
            onCheckedChange={field.onChange}
          />
        )}
      />
    </>
  )
}
