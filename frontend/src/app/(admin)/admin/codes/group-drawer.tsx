"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { Lock } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type CodeGroupInput, codeGroupSchema } from "@/lib/schemas/code"
import type { CodeGroupTree } from "@/types/code"

// 공통코드 그룹 드로어 (moa, specs/004-admin-codes FR-004·FR-008, US1-AC7~AC10). 머리의 "그룹 추가"나 그룹 이름으로 연다
//   만들 때: 그룹 코드, 이름, 설명, 쓰는 곳. 일반 그룹으로 생긴다
//   고칠 때: 이름, 설명, 쓰는 곳. 그룹 코드는 보기만 한다
//   일반 그룹은 "그룹 삭제"(항목도 함께, 확인 창에 항목 수). 시스템 그룹은 지울 수 없다는 안내만
//   group이 있으면 수정, 없으면 추가
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
      usedBy: group?.usedBy ?? "",
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
      const { groupCode, name, description, usedBy } = codeGroupSchema.parse(values)
      const body = { name, description: description || undefined, usedBy: usedBy || undefined }
      return editing
        ? api.v1.code.admin.modifyGroup(group.groupCode, body)
        : api.v1.code.admin.createGroup({ groupCode, ...body })
    },
    onSuccess: async (saved) => {
      await onSaved(saved.groupCode)
      toast.success(t(editing ? "groupModified" : "groupCreated"))
      onOpenChange(false)
    },
    onError,
  })
  const remove = useMutation({
    mutationFn: (groupCode: string) => api.v1.code.admin.removeGroup(groupCode),
    onSuccess: async (_, groupCode) => {
      await onSaved(groupCode)
      toast.success(t("groupRemoved"))
      onOpenChange(false)
    },
    onError,
  })

  const confirmRemove = async () => {
    if (!group) return
    const ok = await confirm({
      title: t("removeGroupTitle", { name: group.name }),
      description: t("removeGroupDescription", { count: group.codes.length }),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate(group.groupCode)
  }

  return (
    <SideDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={t(editing ? "editGroup" : "addGroup")}
      description={t("groupDrawerDescription")}
      dirty={form.formState.isDirty}
      footer={
        <>
          {editing && !group.system && (
            <Button
              type="button"
              variant="destructive"
              className="mr-auto"
              disabled={remove.isPending}
              onClick={() => void confirmRemove()}
            >
              {t("removeGroup")}
            </Button>
          )}
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
          {group?.system && (
            <Alert>
              <Lock aria-hidden="true" />
              <AlertDescription>{t("systemLocked")}</AlertDescription>
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
          <Controller
            name="usedBy"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="group-used-by">{t("usedByLabel")}</FieldLabel>
                <Input
                  {...field}
                  id="group-used-by"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "group-used-by-error" : undefined}
                />
                {fieldState.invalid && (
                  <FieldError id="group-used-by-error" errors={fieldErrorText(fieldState.error)} />
                )}
              </Field>
            )}
          />
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
