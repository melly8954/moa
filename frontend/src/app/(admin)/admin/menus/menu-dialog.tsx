"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
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
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Switch } from "@/components/ui/switch"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type MenuFormInput, type MenuFormOutput, menuSchema } from "@/lib/schemas/menu"
import type { AdminMenu, MenuAudience, MenuVisibility, Program } from "@/types/menu"

// 메뉴 추가·수정 창. menu가 있으면 수정
// 깊이는 2단계까지: 상위 메뉴로 고를 수 있는 것은 1단계 메뉴뿐이고, 하위 메뉴가 있는 메뉴는 상위를 바꿀 수 없다
// moa: audience는 지금 탭의 대상. 새 1단계 메뉴는 이 대상으로 만들고, 하위 메뉴는 부모를 따른다 (FR-007)
//   topLevel은 같은 대상의 1단계 메뉴만 받는다 (상위 메뉴 고르기)
// moa: 경로·권한은 입력하지 않는다. 등록된 프로그램을 고르고, 경로와 권한은 그 프로그램을 보여 주기만 한다 (FR-008·FR-009)
//   종류: 화면 메뉴(프로그램 하나) / 묶음 메뉴(프로그램 없이 하위 메뉴를 묶음, 1단계만)
//   사용자 화면 메뉴는 공개 범위를 고른다. 로그인이 필요한 프로그램이면 "비회원 포함"을 고를 수 없다
export function MenuDialog({
  menu,
  audience,
  topLevel,
  programs,
  open,
  onOpenChange,
  onSaved,
}: {
  menu?: AdminMenu
  audience: MenuAudience
  topLevel: AdminMenu[]
  programs: Program[]
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
}) {
  const t = useTranslations("menuAdmin")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!menu
  const hasChildren = (menu?.children.length ?? 0) > 0
  const menuAudience = menu?.audience ?? audience
  const choices = programs.filter((p) => p.audience === menuAudience)
  const [formError, setFormError] = useState<string>()
  const form = useForm<MenuFormInput, unknown, MenuFormOutput>({
    resolver: zodResolver(menuSchema),
    values: {
      parentId: menu?.parentId ? String(menu.parentId) : "",
      name: menu?.name ?? "",
      kind: menu && !menu.programKey ? "group" : "screen",
      // 없는 프로그램을 가리키던 메뉴는 다시 골라야 한다
      programKey: menu?.program ? (menu.programKey ?? "") : "",
      icon: menu?.icon ?? "",
      sortOrder: String(menu?.sortOrder ?? 0),
      visibility: menu?.visibility ?? "PUBLIC",
      enabled: menu?.enabled ?? true,
    },
  })
  const parentId = useWatch({ control: form.control, name: "parentId" })
  const kind = useWatch({ control: form.control, name: "kind" })
  const programKey = useWatch({ control: form.control, name: "programKey" })
  // 하위 메뉴는 화면 메뉴만 된다. 하위 메뉴가 있는 메뉴는 묶음 메뉴로 둔다
  const child = parentId !== ""
  const screen = child || kind === "screen"
  const program = choices.find((p) => p.key === programKey)
  const memberOnly = program?.signInRequired ?? false

  const save = useMutation({
    mutationFn: (v: MenuFormOutput) => {
      const isScreen = v.parentId !== "" || v.kind === "screen"
      const signInRequired = choices.find((p) => p.key === v.programKey)?.signInRequired ?? false
      const body = {
        parentId: v.parentId ? Number(v.parentId) : undefined,
        name: v.name,
        programKey: isScreen ? v.programKey : undefined,
        icon: v.icon || undefined,
        sortOrder: v.sortOrder,
        audience: menuAudience,
        visibility: isScreen && menuAudience === "USER" ? (signInRequired ? "MEMBER" : v.visibility) : undefined,
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
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof MenuFormInput, { message: "" })
      setFormError(apiError.code === "B001" ? t("depthRule") : describeError(apiError).message)
    },
  })

  const text = (name: "name" | "sortOrder") => (
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
          <DialogDescription>
            {t("dialogDescription")}{" "}
            {t("dialogAudience", { name: t(menuAudience === "USER" ? "audienceUser" : "audienceAdmin") })}
          </DialogDescription>
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
            {editing && menu.missingProgram && (
              <Alert variant="destructive">
                <AlertDescription>{t("missingProgramNote")}</AlertDescription>
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
                      .filter((m) => m.id !== menu?.id && !m.programKey)
                      .map((m) => (
                        <NativeSelectOption key={m.id} value={String(m.id)}>
                          {m.name}
                        </NativeSelectOption>
                      ))}
                  </NativeSelect>
                  <FieldDescription>{hasChildren ? t("hasChildrenNote") : t("parentHint")}</FieldDescription>
                </Field>
              )}
            />
            {!child && (
              <Controller
                name="kind"
                control={form.control}
                render={({ field }) => (
                  <FieldSet>
                    <FieldLegend variant="label">{t("kind")}</FieldLegend>
                    <RadioGroup
                      value={field.value}
                      onValueChange={(value) => field.onChange(value)}
                      disabled={hasChildren}
                    >
                      {(["screen", "group"] as const).map((value) => (
                        <Field key={value} orientation="horizontal">
                          <RadioGroupItem id={`menu-kind-${value}`} value={value} />
                          <FieldLabel htmlFor={`menu-kind-${value}`} className="font-normal">
                            {t(value === "screen" ? "kindScreen" : "kindGroup")}
                          </FieldLabel>
                        </Field>
                      ))}
                    </RadioGroup>
                    <FieldDescription>{t(hasChildren ? "kindHasChildren" : "kindHint")}</FieldDescription>
                  </FieldSet>
                )}
              />
            )}
            {text("name")}
            {screen && (
              <Controller
                name="programKey"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="menu-programKey">{t("program")}</FieldLabel>
                    <NativeSelect
                      id="menu-programKey"
                      value={field.value}
                      onChange={(event) => {
                        field.onChange(event)
                        const next = choices.find((p) => p.key === event.target.value)
                        if (next?.signInRequired) form.setValue("visibility", "MEMBER")
                      }}
                      aria-invalid={fieldState.invalid}
                      aria-describedby={fieldState.invalid ? "menu-programKey-error" : "menu-programKey-info"}
                    >
                      <NativeSelectOption value="">{t("programChoose")}</NativeSelectOption>
                      {choices.map((p) => (
                        <NativeSelectOption key={p.key} value={p.key}>
                          {p.name} ({p.path})
                        </NativeSelectOption>
                      ))}
                    </NativeSelect>
                    {program && (
                      <FieldDescription id="menu-programKey-info">
                        {t("programInfo", {
                          path: program.path,
                          who: program.permission
                            ? t("programPermission", { name: program.permission.name })
                            : t(program.signInRequired ? "programSignIn" : "programPublic"),
                        })}
                      </FieldDescription>
                    )}
                    {fieldState.invalid && (
                      <FieldError id="menu-programKey-error" errors={fieldErrorText(fieldState.error)} />
                    )}
                  </Field>
                )}
              />
            )}
            {screen && menuAudience === "USER" && (
              <Controller
                name="visibility"
                control={form.control}
                render={({ field }) => (
                  <FieldSet>
                    <FieldLegend variant="label">{t("visibility")}</FieldLegend>
                    <RadioGroup
                      value={memberOnly ? "MEMBER" : field.value}
                      onValueChange={(value) => field.onChange(value as MenuVisibility)}
                    >
                      {(["PUBLIC", "MEMBER"] as const).map((value) => (
                        <Field key={value} orientation="horizontal">
                          <RadioGroupItem
                            id={`menu-visibility-${value}`}
                            value={value}
                            disabled={value === "PUBLIC" && memberOnly}
                          />
                          <FieldLabel htmlFor={`menu-visibility-${value}`} className="font-normal">
                            {t(value === "PUBLIC" ? "visibilityPublic" : "visibilityMember")}
                          </FieldLabel>
                        </Field>
                      ))}
                    </RadioGroup>
                    <FieldDescription>{t(memberOnly ? "visibilityMemberOnly" : "visibilityHint")}</FieldDescription>
                  </FieldSet>
                )}
              />
            )}
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
