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
import { Textarea } from "@/components/ui/textarea"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type CodeGroupInput, codeGroupSchema } from "@/lib/schemas/code"
import type { CodeGroupTree } from "@/types/code"

// 공통코드 그룹 패널 (moa, specs/004-admin-codes FR-004·FR-008·FR-009, US1-AC7·AC8·AC9). 트리 맨 아래 "그룹 추가"나 그룹 이름으로 연다
//   만들 때: 그룹 코드, 이름, 설명. 사용 중으로 생긴다
//   고칠 때: 이름, 설명, 사용. 그룹 코드는 보기만 한다. 그룹은 지우지 않고 사용 안 함으로 끈다
//   group이 있으면 수정, 없으면 추가
const toGroupCode = (value: string) =>
  value
    .toUpperCase()
    .replace(/[-\s]/g, "_")
    .replace(/[^A-Z0-9_]/g, "")
    .replace(/^[^A-Z]+/, "")

export function GroupDrawer({
  group,
  open,
  onOpenChange,
  onSaved,
}: {
  group?: CodeGroupTree
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: (groupCode: string) => Promise<unknown>
}) {
  const t = useTranslations("code")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!group
  const [formError, setFormError] = useState<string>()
  const form = useForm<CodeGroupInput>({
    resolver: zodResolver(codeGroupSchema),
    defaultValues: {
      groupCode: group?.groupCode ?? "",
      name: group?.name ?? "",
      description: group?.description ?? "",
      enabled: group?.enabled ?? true,
    },
  })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)
  const onError = (error: unknown) => {
    const apiError = toApiError(error)
    if (apiError.code === "R002") return form.setError("groupCode", { message: "code.groupDuplicate" })
    for (const fieldError of apiError.fieldErrors)
      form.setError(fieldError.field as keyof CodeGroupInput, { message: "" })
    setFormError(describeError(apiError).message)
  }
  const save = useMutation({
    mutationFn: async (values: CodeGroupInput) => {
      const { groupCode, name, description, enabled } = codeGroupSchema.parse(values)
      return editing
        ? api.v1.code.admin.modifyGroup(group.groupCode, { name, description: description || undefined, enabled })
        : api.v1.code.admin.createGroup({ groupCode, name, description: description || undefined })
    },
    onSuccess: async (saved) => {
      await onSaved(saved.groupCode)
      toast.success(t(editing ? "groupModified" : "groupCreated"))
      onOpenChange(false)
    },
    onError,
  })
  return (
    <SideDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={t(editing ? "editGroup" : "addGroup")}
      description={t("groupDrawerDescription")}
      dirty={form.formState.isDirty}
      footer={
        <>
          <Button type="button" variant="outline" onClick={() => void close()}>
            {t("cancel")}
          </Button>
          <Button type="submit" form="code-group-form" disabled={save.isPending}>
            {t("save")}
          </Button>
        </>
      }
    >
      <form
        id="code-group-form"
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
            name="groupCode"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="group-code">{t("groupCode")}</FieldLabel>
                <Input
                  {...field}
                  // 치는 대로 대문자로, -와 빈칸은 _로 바꾸고, 대문자·숫자·밑줄 밖의 글자와 맨 앞의 숫자·밑줄은 받지 않는다 (50자까지)
                  onChange={(e) => field.onChange(toGroupCode(e.target.value))}
                  maxLength={50}
                  autoCapitalize="characters"
                  autoComplete="off"
                  spellCheck={false}
                  id="group-code"
                  className="font-mono"
                  readOnly={editing}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "group-code-error" : "group-code-hint"}
                />
                <FieldDescription id="group-code-hint">
                  {t(editing ? "groupCodeFixed" : "groupCodeHint")}
                </FieldDescription>
                {fieldState.invalid && <FieldError id="group-code-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="name"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="group-name">{t("name")}</FieldLabel>
                <Input
                  {...field}
                  id="group-name"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "group-name-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="group-name-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
          <Controller
            name="description"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="group-description">{t("description")}</FieldLabel>
                <Textarea
                  {...field}
                  id="group-description"
                  rows={3}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "group-description-error" : undefined}
                />
                {fieldState.invalid && (
                  <FieldError id="group-description-error" errors={fieldErrorText(fieldState.error)} />
                )}
              </Field>
            )}
          />
          {editing && (
            <Controller
              name="enabled"
              control={form.control}
              render={({ field }) => (
                <Field>
                  <div className="flex items-center gap-2">
                    <Switch
                      id="group-enabled"
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      aria-label={t("enabled")}
                      aria-describedby="group-enabled-hint"
                    />
                    <FieldLabel htmlFor="group-enabled">{t("enabled")}</FieldLabel>
                  </div>
                  <FieldDescription id="group-enabled-hint">{t("groupEnabledHint")}</FieldDescription>
                </Field>
              )}
            />
          )}
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
