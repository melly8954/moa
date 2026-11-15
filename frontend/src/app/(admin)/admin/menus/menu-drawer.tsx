"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer } from "@/components/kit/side-drawer"
import { MENU_ICONS } from "@/components/menu/icons"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
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
import { isGroup } from "./menu-order"

// 메뉴 드로어 (moa, specs/001-admin-menus FR-010·FR-012, research R22). 이름을 누르거나 "메뉴 추가"로 연다
//   경로·권한은 입력하지 않는다. 등록된 프로그램을 고르고, 경로와 권한은 그 프로그램을 보여 주기만 한다 (FR-008·FR-009)
//   종류: 화면 메뉴(프로그램 하나) / 묶음 메뉴(하위 메뉴를 묶음, 1단계만). 하위 메뉴가 있는 묶음은 종류를 바꾸지 않는다
//   순서와 상위 메뉴는 표에서 끌어 정한다. 새 메뉴만 넣을 곳(1단계 또는 묶음)을 고르고, 그 맨 뒤에 둔다
//   사용자 화면 메뉴는 공개 범위를 고른다. 로그인이 필요한 프로그램이면 "비회원 포함"을 고를 수 없다
//   menu가 있으면 수정, open이면서 menu가 없으면 추가
export function MenuDrawer({
  menu,
  open,
  audience,
  tree,
  programs,
  onOpenChange,
  onSaved,
}: {
  menu?: AdminMenu
  open: boolean
  audience: MenuAudience
  tree: AdminMenu[]
  programs: Program[]
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
  const groups = tree.filter((m) => isGroup(m) && m.id !== menu?.id)
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
      visibility: menu?.visibility ?? "PUBLIC",
      enabled: menu?.enabled ?? true,
    },
  })
  const parentId = useWatch({ control: form.control, name: "parentId" })
  const kind = useWatch({ control: form.control, name: "kind" })
  const programKey = useWatch({ control: form.control, name: "programKey" })
  const child = parentId !== ""
  const screen = child || kind === "screen"
  const program = choices.find((p) => p.key === programKey)
  const memberOnly = program?.signInRequired ?? false

  // 새 메뉴는 넣을 곳의 맨 뒤 (10 단위)
  const lastSortOrder = (parent: string) => {
    const siblings = parent === "" ? tree : (tree.find((m) => String(m.id) === parent)?.children ?? [])
    return siblings.reduce((max, m) => Math.max(max, m.sortOrder), 0) + 10
  }

  const save = useMutation({
    mutationFn: (v: MenuFormOutput) => {
      const isScreen = v.parentId !== "" || v.kind === "screen"
      const signInRequired = choices.find((p) => p.key === v.programKey)?.signInRequired ?? false
      const body = {
        parentId: v.parentId ? Number(v.parentId) : undefined,
        name: v.name,
        programKey: isScreen ? v.programKey : undefined,
        icon: v.icon || undefined,
        sortOrder: editing ? menu.sortOrder : lastSortOrder(v.parentId),
        audience: menuAudience,
        visibility: isScreen && menuAudience === "USER" ? (signInRequired ? "MEMBER" : v.visibility) : undefined,
      } as const
      return editing
        ? api.v1.menu.admin.modify(menu.id, { ...body, enabled: v.enabled })
        : api.v1.menu.admin.create(body)
    },
    onSuccess: async () => {
      await onSaved()
      toast.success(t(editing ? "modified" : "created"))
      form.reset()
      onOpenChange(false)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      for (const fieldError of apiError.fieldErrors)
        form.setError(fieldError.field as keyof MenuFormInput, { message: "" })
      setFormError(apiError.code === "B001" ? t("depthRule") : describeError(apiError).message)
    },
  })

  return (
    <SideDrawer
      open={open}
      onOpenChange={(next) => {
        if (!next) setFormError(undefined)
        onOpenChange(next)
      }}
      title={t(editing ? "edit" : "add")}
      description={t("drawerDescription", {
        name: t(menuAudience === "USER" ? "audienceUser" : "audienceAdmin"),
      })}
      dirty={form.formState.isDirty}
      footer={
        <>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            {t("cancel")}
          </Button>
          <Button type="submit" form="menu-form" disabled={save.isPending}>
            {t("save")}
          </Button>
        </>
      }
    >
      <form
        id="menu-form"
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
          {!editing && (
            <Controller
              name="parentId"
              control={form.control}
              render={({ field }) => (
                <Field>
                  <FieldLabel htmlFor="menu-parentId">{t("placeIn")}</FieldLabel>
                  <NativeSelect id="menu-parentId" value={field.value} onChange={field.onChange}>
                    <NativeSelectOption value="">{t("topLevel")}</NativeSelectOption>
                    {groups.map((m) => (
                      <NativeSelectOption key={m.id} value={String(m.id)}>
                        {m.name}
                      </NativeSelectOption>
                    ))}
                  </NativeSelect>
                  <FieldDescription>{t("placeInHint")}</FieldDescription>
                </Field>
              )}
            />
          )}
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
          <Controller
            name="name"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="menu-name">{t("name")}</FieldLabel>
                <Input
                  {...field}
                  id="menu-name"
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "menu-name-error" : undefined}
                />
                {fieldState.invalid && <FieldError id="menu-name-error" errors={fieldErrorText(fieldState.error)} />}
              </Field>
            )}
          />
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
          {editing && (
            <Controller
              name="enabled"
              control={form.control}
              render={({ field }) => (
                <Field orientation="horizontal">
                  <Switch id="menu-enabled" checked={field.value} onCheckedChange={field.onChange} />
                  <FieldLabel htmlFor="menu-enabled">{t("enabled")}</FieldLabel>
                </Field>
              )}
            />
          )}
        </FieldGroup>
      </form>
    </SideDrawer>
  )
}
