"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type RoleInput, roleSchema } from "@/lib/schemas/role"
import type { Permission, Role } from "@/types/role"

// 역할 추가·수정 창. role이 있으면 수정(코드는 바꾸지 않는다)
// 권한은 묶음(group)별 체크 목록이다. 내가 가진 권한 밖을 다루면 서버가 A005로 막는다
export function RoleDialog({
  role,
  open,
  onOpenChange,
}: {
  role?: Role
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!role
  const permissions = useQuery({ queryKey: ["admin", "permissions"], queryFn: api.v1.role.permissions, enabled: open })
  const [formError, setFormError] = useState<string>()
  const form = useForm<RoleInput>({
    resolver: zodResolver(roleSchema),
    values: {
      code: role?.code ?? "",
      name: role?.name ?? "",
      description: role?.description ?? "",
      permissions: role?.permissions ?? [],
    },
  })

  const save = useMutation({
    mutationFn: ({ code, name, description, permissions: keys }: RoleInput) =>
      editing
        ? api.v1.role.modify(role.id, { name, description: description || undefined, permissions: keys })
        : api.v1.role.create({ code, name, description: description || undefined, permissions: keys }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t(editing ? "modified" : "created"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("code", { message: "role.duplicate" })
      for (const fieldError of apiError.fieldErrors) form.setError(fieldError.field as keyof RoleInput, { message: "" })
      // A005: 내가 가진 권한 밖의 권한을 넣거나 뺐다 (권한 상승 방지)
      setFormError(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  // 권한을 묶음별로
  const groups = new Map<string, Permission[]>()
  for (const p of permissions.data ?? []) groups.set(p.group, [...(groups.get(p.group) ?? []), p])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t(editing ? "edit" : "add")}</DialogTitle>
          <DialogDescription>{t("dialogDescription")}</DialogDescription>
        </DialogHeader>
        <form
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
            {(["code", "name", "description"] as const).map((name) => (
              <Controller
                key={name}
                name={name}
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor={`role-${name}`}>{t(name)}</FieldLabel>
                    <Input
                      {...field}
                      id={`role-${name}`}
                      disabled={name === "code" && editing}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? `role-${name}-error` : undefined}
                    />
                    {fieldState.invalid && (
                      <FieldError id={`role-${name}-error`} errors={fieldErrorText(fieldState.error)} />
                    )}
                  </Field>
                )}
              />
            ))}
            <Controller
              name="permissions"
              control={form.control}
              render={({ field }) => (
                <FieldSet>
                  <FieldLegend variant="label">{t("permissions")}</FieldLegend>
                  {[...groups].map(([group, list]) => (
                    <div key={group} className="flex flex-col gap-2">
                      <p className="text-sm font-medium text-muted-foreground">{group}</p>
                      {list.map((p) => (
                        <Field key={p.key} orientation="horizontal">
                          <Checkbox
                            id={`permission-${p.key}`}
                            aria-label={p.name}
                            checked={field.value.includes(p.key)}
                            onCheckedChange={(checked) =>
                              field.onChange(checked ? [...field.value, p.key] : field.value.filter((k) => k !== p.key))
                            }
                          />
                          <FieldLabel htmlFor={`permission-${p.key}`} className="font-normal">
                            {p.name} <span className="font-mono text-xs text-muted-foreground">{p.key}</span>
                          </FieldLabel>
                        </Field>
                      ))}
                    </div>
                  ))}
                </FieldSet>
              )}
            />
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
