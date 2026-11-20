"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { useDrawerClose } from "@/components/kit/side-drawer"
import { ResponsivePanel as SideDrawer } from "@/components/kit/detail-panel"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type CodeItemInput, codeItemSchema } from "@/lib/schemas/code"
import type { Code, CodeGroup } from "@/types/code"

// 공통코드 항목 드로어 (moa, specs/004-admin-codes FR-005·FR-006, US1-AC2·AC3). 이름을 누르거나 그룹의 "항목 추가"로 연다
//   만들 때: 이름과 코드(비우면 자동 순번). 그룹 맨 뒤에 생긴다
//   고칠 때: 이름과 사용 여부. 코드는 보기만 한다. 순서는 표에서 끌어 바꾼다
//   code가 있으면 수정, 없으면 추가
export function CodeDrawer({
  group,
  code,
  open,
  onOpenChange,
  onSaved,
}: {
  group?: CodeGroup
  code?: Code
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: (groupCode: string) => Promise<unknown>
}) {
  const t = useTranslations("code")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!code
  const [formError, setFormError] = useState<string>()
  const form = useForm<CodeItemInput>({
    resolver: zodResolver(codeItemSchema),
    defaultValues: { code: code?.code ?? "", name: code?.name ?? "", enabled: code?.enabled ?? true },
  })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)
  const save = useMutation({
    mutationFn: (values: CodeItemInput) => {
      const groupCode = group!.groupCode
      const { code: value, name, enabled } = codeItemSchema.parse(values)
      return editing
        ? api.v1.code.admin.modifyCode(groupCode, code.code, { name, enabled })
        : api.v1.code.admin.createCode(groupCode, { code: value || undefined, name })
    },
    onSuccess: async () => {
      await onSaved(group!.groupCode)
      toast.success(t(editing ? "codeModified" : "codeCreated"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("code", { message: "code.duplicate" })
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof CodeItemInput, { message: "" })
      setFormError(describeError(apiError).message)
    },
  })

  return (
    <SideDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={t(editing ? "editCode" : "addCode")}
      description={group ? t("drawerDescription", { name: group.name }) : undefined}
      dirty={form.formState.isDirty}
      footer={
        <>
          <Button type="button" variant="outline" onClick={() => void close()}>
            {t("cancel")}
          </Button>
          <Button type="submit" form="code-form" disabled={save.isPending}>
            {t("save")}
          </Button>
        </>
      }
    >
      <form
        id="code-form"
        noValidate
        onSubmit={form.handleSubmit((values) => {
          setFormError(undefined)
          save.mutate(values)
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
                <FieldLabel htmlFor="code-name">{t("name")}</FieldLabel>
                <Input
                  {...field}
                  id="code-name"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "code-name-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="code-name-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="code"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="code-code">{t("code")}</FieldLabel>
                <Input
                  {...field}
                  // 치는 대로 대문자로, -와 빈칸은 _로 바꾸고, 대문자·숫자·밑줄 밖의 글자는 받지 않는다 (50자까지)
                  onChange={(e) =>
                    field.onChange(
                      e.target.value
                        .toUpperCase()
                        .replace(/[-\s]/g, "_")
                        .replace(/[^A-Z0-9_]/g, ""),
                    )
                  }
                  maxLength={50}
                  autoCapitalize="characters"
                  autoComplete="off"
                  spellCheck={false}
                  id="code-code"
                  className="font-mono"
                  readOnly={editing}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "code-code-error" : "code-code-hint"}
                />
                <FieldDescription id="code-code-hint">{t(editing ? "codeFixed" : "codeHint")}</FieldDescription>
                {fieldState.invalid && <FieldError id="code-code-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          {editing && (
            <Controller
              name="enabled"
              control={form.control}
              render={({ field }) => (
                <Field orientation="horizontal">
                  <Switch
                    id="code-enabled"
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    aria-label={t("enabled")}
                  />
                  <FieldLabel htmlFor="code-enabled">{t("enabled")}</FieldLabel>
                </Field>
              )}
            />
          )}
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
