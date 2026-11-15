"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { SideDrawer, useDrawerClose } from "@/components/kit/side-drawer"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { type RoleInput, roleSchema } from "@/lib/schemas/role"
import type { Role } from "@/types/role"
import { PermissionTreePicker } from "./permission-tree"
import { RoleMembers } from "./role-members"

export type RoleDrawerTab = "basic" | "members"

// 역할 드로어 (moa, specs/003-admin-roles FR-004~FR-006, research R12·R13)
//   탭 "기본·메뉴"(코드·이름·설명·관리 메뉴 트리)와 "구성원"(검색·쪽 나눔·일괄 더하기·빼기)
//   새 역할은 "기본·메뉴"만 있고, 저장하면 onCreated로 그 역할을 다시 열어 "구성원"이 보인다
//   코드는 만든 뒤 바꾸지 않는다. 내가 가진 권한 밖을 다루면 서버가 A005로 막는다 (권한 상승 방지)
export function RoleDrawer({
  role,
  open,
  tab,
  onTabChange,
  onOpenChange,
  onCreated,
}: {
  role?: Role
  open: boolean
  tab: RoleDrawerTab
  onTabChange: (tab: RoleDrawerTab) => void
  onOpenChange: (open: boolean) => void
  onCreated: (role: Role) => void
}) {
  const t = useTranslations("role")
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const editing = !!role
  const tree = useQuery({
    queryKey: ["admin", "roles", "permission-tree"],
    queryFn: api.v1.role.permissionTree,
    enabled: open,
  })
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
  const close = useDrawerClose(form.formState.isDirty, onOpenChange)

  const save = useMutation({
    mutationFn: ({ code, name, description, permissions: keys }: RoleInput) =>
      editing
        ? api.v1.role.modify(role.id, { name, description: description || undefined, permissions: keys })
        : api.v1.role.create({ code, name, description: description || undefined, permissions: keys }),
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "roles"] })
      toast.success(t(editing ? "modified" : "created"))
      form.reset()
      if (!editing) onCreated(saved)
    },
    onError: (error) => {
      const apiError = toApiError(error)
      if (apiError.code === "R002") return form.setError("code", { message: "role.duplicate" })
      for (const fieldError of apiError.fieldErrors) form.setError(fieldError.field as keyof RoleInput, { message: "" })
      // A005: 내가 가진 권한 밖의 권한을 넣거나 뺐다 (권한 상승 방지)
      setFormError(apiError.code === "A005" ? t("grantDenied") : describeError(apiError).message)
    },
  })

  const basic = (
    <form
      id="role-form"
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
          render={({ field }) =>
            tree.data ? <PermissionTreePicker tree={tree.data} value={field.value} onChange={field.onChange} /> : <></>
          }
        />
      </FieldGroup>
    </form>
  )

  return (
    <SideDrawer
      open={open}
      onOpenChange={(next) => {
        if (!next) setFormError(undefined)
        onOpenChange(next)
      }}
      title={editing ? role.name : t("add")}
      description={t("dialogDescription")}
      dirty={form.formState.isDirty}
      footer={
        tab === "basic" ? (
          <>
            <Button type="button" variant="outline" onClick={() => void close()}>
              {t("cancel")}
            </Button>
            <Button type="submit" form="role-form" disabled={save.isPending}>
              {t("save")}
            </Button>
          </>
        ) : undefined
      }
    >
      {editing ? (
        <Tabs value={tab} onValueChange={(value) => onTabChange(value as RoleDrawerTab)}>
          <TabsList aria-label={t("drawerTabs")}>
            <TabsTrigger value="basic">{t("tabBasic")}</TabsTrigger>
            <TabsTrigger value="members">{t("tabMembers")}</TabsTrigger>
          </TabsList>
          <TabsContent value="basic" className="pt-3">
            {basic}
          </TabsContent>
          <TabsContent value="members" className="pt-3">
            <RoleMembers role={role} />
          </TabsContent>
        </Tabs>
      ) : (
        basic
      )}
    </SideDrawer>
  )
}
