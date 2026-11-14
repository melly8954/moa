"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { type Control, Controller, type FieldValues, type Path, useForm, type UseFormReturn } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import {
  type CodeFormInput,
  type CodeFormOutput,
  codeCreateSchema,
  type CodeGroupInput,
  codeGroupSchema,
} from "@/lib/schemas/code"
import type { Code } from "@/types/code"

// 코드 관리 화면의 그룹 추가·코드 추가·코드 수정 창

function TextField<T extends FieldValues, TOut extends FieldValues = T>({
  control,
  name,
  label,
  disabled,
  inputMode,
}: {
  control: Control<T, unknown, TOut>
  name: Path<T>
  label: string
  disabled?: boolean
  inputMode?: React.HTMLAttributes<HTMLInputElement>["inputMode"]
}) {
  const fieldErrorText = useFieldErrorText()
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState }) => (
        <Field data-invalid={fieldState.invalid}>
          <FieldLabel htmlFor={name}>{label}</FieldLabel>
          <Input
            {...field}
            id={name}
            disabled={disabled}
            inputMode={inputMode}
            aria-invalid={fieldState.invalid}
            aria-describedby={fieldState.invalid ? `${name}-error` : undefined}
          />
          {fieldState.invalid && <FieldError id={`${name}-error`} errors={fieldErrorText(fieldState.error)} />}
        </Field>
      )}
    />
  )
}

// 서버 오류를 폼에 보인다: R002(이미 있음)는 코드 칸에, 칸 오류(V001)는 그 칸에, 나머지는 위에 문구로
function useServerError<T extends FieldValues, TOut extends FieldValues = T>(
  form: UseFormReturn<T, unknown, TOut>,
  duplicateField: Path<T>,
) {
  const { describeError } = useErrorMessages()
  const [formError, setFormError] = useState<string>()
  const handle = (error: unknown) => {
    const apiError = toApiError(error)
    if (apiError.code === "R002") {
      form.setError(duplicateField, { message: "code.duplicate" })
      return
    }
    for (const fieldError of apiError.fieldErrors) form.setError(fieldError.field as Path<T>, { message: "" })
    setFormError(describeError(apiError).message)
  }
  return { formError, setFormError, handle }
}

export function GroupDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated: (groupCode: string) => void
}) {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const form = useForm<CodeGroupInput>({
    resolver: zodResolver(codeGroupSchema),
    defaultValues: { groupCode: "", name: "", description: "" },
  })
  const server = useServerError(form, "groupCode")
  const create = useMutation({
    mutationFn: (values: CodeGroupInput) => api.v1.code.admin.createGroup(codeGroupSchema.parse(values)),
    onSuccess: async (group) => {
      await queryClient.invalidateQueries({
        queryKey: ["admin", "code-groups"],
      })
      toast.success(t("groupCreated"))
      onOpenChange(false)
      form.reset()
      onCreated(group.groupCode)
    },
    onError: server.handle,
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("addGroup")}</DialogTitle>
          <DialogDescription>{t("codeRule")}</DialogDescription>
        </DialogHeader>
        <form
          noValidate
          onSubmit={form.handleSubmit((values) => {
            server.setFormError(undefined)
            create.mutate(values)
          })}
        >
          <FieldGroup>
            {server.formError && (
              <Alert variant="destructive">
                <AlertDescription>{server.formError}</AlertDescription>
              </Alert>
            )}
            <TextField control={form.control} name="groupCode" label={t("groupCode")} />
            <TextField control={form.control} name="name" label={t("name")} />
            <TextField control={form.control} name="description" label={t("description")} />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={create.isPending}>
                {t("save")}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// code가 있으면 수정(코드 값은 바꾸지 않는다), 없으면 추가
export function CodeDialog({
  groupCode,
  code,
  open,
  onOpenChange,
}: {
  groupCode: string
  code?: Code
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const t = useTranslations("code")
  const queryClient = useQueryClient()
  const editing = !!code
  const form = useForm<CodeFormInput, unknown, CodeFormOutput>({
    resolver: zodResolver(codeCreateSchema),
    values: {
      code: code?.code ?? "",
      name: code?.name ?? "",
      sortOrder: String(code?.sortOrder ?? 0),
      enabled: code?.enabled ?? true,
    },
  })
  const server = useServerError(form, "code")
  const save = useMutation({
    mutationFn: (values: CodeFormOutput) =>
      editing
        ? api.v1.code.admin.modifyCode(groupCode, values.code, {
            name: values.name,
            sortOrder: values.sortOrder,
            enabled: values.enabled,
          })
        : api.v1.code.admin.createCode(groupCode, {
            code: values.code,
            name: values.name,
            sortOrder: values.sortOrder,
          }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["admin", "codes", groupCode],
        }),
        queryClient.invalidateQueries({ queryKey: ["codes", groupCode] }),
      ])
      toast.success(t(editing ? "codeModified" : "codeCreated"))
      onOpenChange(false)
    },
    onError: server.handle,
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t(editing ? "editCode" : "addCode")}</DialogTitle>
          <DialogDescription>{t("codeRule")}</DialogDescription>
        </DialogHeader>
        <form
          noValidate
          onSubmit={form.handleSubmit((values) => {
            server.setFormError(undefined)
            save.mutate(values)
          })}
        >
          <FieldGroup>
            {server.formError && (
              <Alert variant="destructive">
                <AlertDescription>{server.formError}</AlertDescription>
              </Alert>
            )}
            <TextField control={form.control} name="code" label={t("code")} disabled={editing} />
            <TextField control={form.control} name="name" label={t("name")} />
            <TextField control={form.control} name="sortOrder" label={t("sortOrder")} inputMode="numeric" />
            {editing && (
              <Controller
                name="enabled"
                control={form.control}
                render={({ field }) => (
                  <Field orientation="horizontal">
                    <Switch
                      id="enabled"
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      aria-label={t("enabled")}
                    />
                    <FieldLabel htmlFor="enabled">{t("enabled")}</FieldLabel>
                  </Field>
                )}
              />
            )}
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={save.isPending}>
                {t("save")}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
