"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { MENU_ICONS } from "@/components/menu/icons"
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
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Switch } from "@/components/ui/switch"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type MenuFormInput, type MenuFormOutput, menuSchema } from "@/lib/schemas/menu"
import type { AdminMenu } from "@/types/menu"

// 메뉴 추가·수정 창. menu가 있으면 수정
// 깊이는 2단계까지: 상위 메뉴로 고를 수 있는 것은 1단계 메뉴뿐이고, 하위 메뉴가 있는 메뉴는 상위를 바꿀 수 없다
export function MenuDialog({
  menu,
  topLevel,
  open,
  onOpenChange,
  onSaved,
}: {
  menu?: AdminMenu
  topLevel: AdminMenu[]
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
}) {
  const t = useTranslations("menuAdmin")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!menu
  const hasChildren = (menu?.children.length ?? 0) > 0
  const [formError, setFormError] = useState<string>()
  const form = useForm<MenuFormInput, unknown, MenuFormOutput>({
    resolver: zodResolver(menuSchema),
    values: {
      parentId: menu?.parentId ? String(menu.parentId) : "",
      name: menu?.name ?? "",
      path: menu?.path ?? "",
      icon: menu?.icon ?? "",
      sortOrder: String(menu?.sortOrder ?? 0),
      permission: menu?.permission ?? "",
      enabled: menu?.enabled ?? true,
    },
  })

  const save = useMutation({
    mutationFn: (v: MenuFormOutput) => {
      const body = {
        parentId: v.parentId ? Number(v.parentId) : undefined,
        name: v.name,
        path: v.path || undefined,
        icon: v.icon || undefined,
        sortOrder: v.sortOrder,
        permission: v.permission || undefined,
      }
      return editing
        ? api.v1.menu.admin.modify(menu.id, { ...body, enabled: v.enabled })
        : api.v1.menu.admin.create(body)
    },
    onSuccess: async () => {
      await onSaved()
      toast.success(t(editing ? "modified" : "created"))
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("path", { message: "menuAdmin.duplicatePath" })
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof MenuFormInput, { message: "" })
      setFormError(apiError.code === "B001" ? t("depthRule") : describeError(apiError).message)
    },
  })

  const text = (name: "name" | "path" | "sortOrder" | "permission", hint?: string) => (
    <Controller
      name={name}
      control={form.control}
      render={({ field, fieldState }) => (
        <Field data-invalid={fieldState.invalid}>
          <FieldLabel htmlFor={`menu-${name}`}>{t(name)}</FieldLabel>
          <Input
            {...field}
            id={`menu-${name}`}
            inputMode={name === "sortOrder" ? "numeric" : undefined}
            aria-invalid={fieldState.invalid}
            aria-describedby={fieldState.invalid ? `menu-${name}-error` : undefined}
          />
          {hint && <FieldDescription>{hint}</FieldDescription>}
          {fieldState.invalid && <FieldError id={`menu-${name}-error`} errors={fieldErrorText(fieldState.error)} />}
        </Field>
      )}
    />
  )

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
            <Controller
              name="parentId"
              control={form.control}
              render={({ field }) => (
                <Field>
                  <FieldLabel htmlFor="menu-parentId">{t("parent")}</FieldLabel>
                  <NativeSelect id="menu-parentId" value={field.value} onChange={field.onChange} disabled={hasChildren}>
                    <NativeSelectOption value="">{t("topLevel")}</NativeSelectOption>
                    {topLevel
                      .filter((m) => m.id !== menu?.id)
                      .map((m) => (
                        <NativeSelectOption key={m.id} value={String(m.id)}>
                          {m.name}
                        </NativeSelectOption>
                      ))}
                  </NativeSelect>
                  {hasChildren && <FieldDescription>{t("hasChildrenNote")}</FieldDescription>}
                </Field>
              )}
            />
            {text("name")}
            {text("path", t("pathHint"))}
            <Controller
              name="icon"
              control={form.control}
              render={({ field }) => (
                <Field>
                  <FieldLabel htmlFor="menu-icon">{t("icon")}</FieldLabel>
                  <NativeSelect id="menu-icon" value={field.value} onChange={field.onChange}>
                    <NativeSelectOption value="">{t("noIcon")}</NativeSelectOption>
                    {Object.keys(MENU_ICONS).map((name) => (
                      <NativeSelectOption key={name} value={name}>
                        {name}
                      </NativeSelectOption>
                    ))}
                  </NativeSelect>
                </Field>
              )}
            />
            {text("sortOrder")}
            {text("permission", t("permissionHint"))}
            {editing && (
              <Controller
                name="enabled"
                control={form.control}
                render={({ field }) => (
                  <Field orientation="horizontal">
                    <Switch
                      id="menu-enabled"
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      aria-label={t("enabled")}
                    />
                    <FieldLabel htmlFor="menu-enabled">{t("enabled")}</FieldLabel>
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
