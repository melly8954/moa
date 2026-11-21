"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { ResponsivePanel } from "@/components/kit/detail-panel"
import { EnabledSwitch } from "@/components/kit/enabled-switch"
import { useDrawerClose } from "@/components/kit/side-drawer"
import { IconPicker } from "@/components/menu/icon-picker"
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
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type MenuFormInput, type MenuFormOutput, menuSchema } from "@/lib/schemas/menu"
import type { AdminMenu, MenuAudience, MenuVisibility, Screen } from "@/types/menu"
import { isGroup } from "./menu-order"

// 메뉴 상세 (moa, specs/001-admin-menus FR-009·FR-010·FR-012, research R25). 이름을 누르거나 "메뉴 추가"로 연다
//   넓은 폭은 오른쪽 상세 칸, 좁은 폭은 드로어 (ResponsivePanel, 공통코드와 같다)
//   경로·권한은 입력하지 않는다. 등록된 연결 화면을 "연결 화면"으로 고르고, 경로와 권한은 보여 주기만 한다 (FR-008·FR-009)
//   "종류" 칸은 없다. 연결 화면을 고르면 화면 메뉴, 비우면 상위 메뉴다. 상위 메뉴 아래에 두면 연결 화면을 골라야 한다
//   "상위 메뉴" 칸은 고치기에도 보인다. 상위 메뉴를 바꾸면 새 상위 메뉴의 맨 뒤에 둔다
//   하위 메뉴가 있는 메뉴는 맨 위의 상위 메뉴로 고정한다 (상위 메뉴·연결 화면 칸을 바꾸지 않는다)
//   사용자 화면 메뉴는 공개 범위를 고른다. 로그인이 필요한 연결 화면이면 "비회원 포함"을 고를 수 없다
//   menu가 있으면 수정, open이면서 menu가 없으면 추가. 추가는 defaultParentId를 상위 메뉴 칸의 처음 값으로 쓴다
export function MenuDrawer({
  menu,
  open,
  audience,
  tree,
  screens,
  defaultParentId,
  onOpenChange,
  onSaved,
}: {
  menu?: AdminMenu
  open: boolean
  audience: MenuAudience
  tree: AdminMenu[]
  screens: Screen[]
  defaultParentId?: number
  onOpenChange: (open: boolean) => void
  onSaved: () => Promise<unknown>
}) {
  const t = useTranslations("menuAdmin")
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!menu
  const hasChildren = (menu?.children.length ?? 0) > 0
  const menuAudience = menu?.audience ?? audience
  const choices = screens.filter((p) => p.audience === menuAudience)
  const groups = tree.filter((m) => isGroup(m) && m.id !== menu?.id)
  const [formError, setFormError] = useState<string>()
  const form = useForm<MenuFormInput, unknown, MenuFormOutput>({
    resolver: zodResolver(menuSchema),
    values: {
      parentId: menu ? (menu.parentId ? String(menu.parentId) : "") : defaultParentId ? String(defaultParentId) : "",
      name: menu?.name ?? "",
      // 없는 연결 화면을 가리키던 메뉴는 다시 골라야 한다
      screenKey: menu?.screen ? (menu.screenKey ?? "") : "",
      icon: menu?.icon ?? "",
      visibility: menu?.visibility ?? "PUBLIC",
      enabled: menu?.enabled ?? true,
    },
  })
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)
  const parentId = useWatch({ control: form.control, name: "parentId" })
  const screenKey = useWatch({ control: form.control, name: "screenKey" })
  const child = parentId !== ""
  const screenMenu = screenKey !== ""
  const screen = choices.find((p) => p.key === screenKey)
  const memberOnly = screen?.signInRequired ?? false

  // 새 자리의 맨 뒤 (10 단위)
  const lastSortOrder = (parent: string) => {
    const siblings = parent === "" ? tree : (tree.find((m) => String(m.id) === parent)?.children ?? [])
    return siblings.filter((m) => m.id !== menu?.id).reduce((max, m) => Math.max(max, m.sortOrder), 0) + 10
  }

  const save = useMutation({
    mutationFn: (v: MenuFormOutput) => {
      const isScreen = v.screenKey !== ""
      const sameParent = editing && v.parentId === (menu.parentId ? String(menu.parentId) : "")
      const signInRequired = choices.find((p) => p.key === v.screenKey)?.signInRequired ?? false
      const body = {
        parentId: v.parentId ? Number(v.parentId) : undefined,
        name: v.name,
        screenKey: isScreen ? v.screenKey : undefined,
        icon: v.icon || undefined,
        sortOrder: sameParent ? menu.sortOrder : lastSortOrder(v.parentId),
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
    <ResponsivePanel
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
          <Button type="button" variant="outline" onClick={() => void close()}>
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
          {editing && menu.missingScreen && (
            <Alert variant="destructive">
              <AlertDescription>{t("missingScreenNote")}</AlertDescription>
            </Alert>
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
                {/* 사용 토글은 메뉴명 칸 바로 아래 오른쪽 (FR-010, 2026-11-21). 고칠 때만 */}
                {editing && (
                  <Controller
                    name="enabled"
                    control={form.control}
                    render={({ field: enabled }) => (
                      <EnabledSwitch
                        id="menu-enabled"
                        label={t("enabled")}
                        checked={enabled.value}
                        onCheckedChange={enabled.onChange}
                      />
                    )}
                  />
                )}
              </Field>
            )}
          />
          <Controller
            name="parentId"
            control={form.control}
            render={({ field }) => (
              <Field>
                <FieldLabel htmlFor="menu-parentId">{t("parent")}</FieldLabel>
                <NativeSelect
                  id="menu-parentId"
                  value={field.value}
                  onChange={field.onChange}
                  disabled={hasChildren}
                  aria-describedby="menu-parentId-info"
                >
                  <NativeSelectOption value="">{t("topLevel")}</NativeSelectOption>
                  {groups.map((m) => (
                    <NativeSelectOption key={m.id} value={String(m.id)}>
                      {m.name}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
                <FieldDescription id="menu-parentId-info">
                  {t(hasChildren ? "parentHasChildren" : "parentHint")}
                </FieldDescription>
              </Field>
            )}
          />
          <Controller
            name="screenKey"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="menu-screenKey">{t("screen")}</FieldLabel>
                <NativeSelect
                  id="menu-screenKey"
                  value={field.value}
                  disabled={hasChildren}
                  onChange={(event) => {
                    field.onChange(event)
                    const next = choices.find((p) => p.key === event.target.value)
                    if (next?.signInRequired) form.setValue("visibility", "MEMBER")
                  }}
                  aria-invalid={fieldState.invalid}
                  aria-describedby={fieldState.invalid ? "menu-screenKey-error" : "menu-screenKey-info"}
                >
                  <NativeSelectOption value="">{t(child ? "screenChoose" : "screenNone")}</NativeSelectOption>
                  {choices.map((p) => (
                    <NativeSelectOption key={p.key} value={p.key}>
                      {p.name} ({p.path})
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
                <FieldDescription id="menu-screenKey-info">
                  {screen
                    ? t("screenInfo", {
                        path: screen.path,
                        who: screen.permission
                          ? t("screenPermission", { name: screen.permission.name })
                          : t(screen.signInRequired ? "screenSignIn" : "screenPublic"),
                      })
                    : t(hasChildren ? "screenHasChildren" : child ? "screenRequiredHint" : "screenNoneHint")}
                </FieldDescription>
                {fieldState.invalid && (
                  <FieldError id="menu-screenKey-error" errors={fieldErrorText(fieldState.error)} />
                )}
              </Field>
            )}
          />
          {screenMenu && menuAudience === "USER" && (
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
                {/* 이름으로 찾고 미리보기가 붙은 선택 상자 (FR-010, 2026-11-21) */}
                <IconPicker id="menu-icon" value={field.value ?? ""} onChange={field.onChange} />
              </Field>
            )}
          />
        </FieldGroup>
      </form>
    </ResponsivePanel>
  )
}
