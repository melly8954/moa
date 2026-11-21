"use client"

import { useTranslations } from "next-intl"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import type { PermissionTree, PermissionTreeNode } from "@/types/role"

// 역할 창의 권한 고르기 (moa, specs/003-admin-roles FR-004, research R10)
//   관리 메뉴 트리를 체크한다. 체크한 메뉴의 연결 화면 권한이 역할에 담긴다 (value는 권한 키 목록)
//   같은 권한을 쓰는 메뉴는 함께 체크된다. 묶음 메뉴를 체크하면 하위 메뉴가 모두 체크된다
//   어느 메뉴도 가리키지 않는 관리 연결 화면은 맨 아래 "메뉴에 없는 화면"에 둔다
//   disabled면 보기만 한다 (최고 관리자 역할, 내가 다룰 수 없는 역할. specs/003-admin-roles FR-010, research R19)
export function PermissionTreePicker({
  tree,
  value,
  onChange,
  disabled = false,
}: {
  tree: PermissionTree
  value: string[]
  onChange: (value: string[]) => void
  disabled?: boolean
}) {
  const t = useTranslations("role")
  const toggle = (keys: string[], checked: boolean) =>
    onChange(checked ? [...new Set([...value, ...keys])] : value.filter((k) => !keys.includes(k)))

  const keysOf = (node: PermissionTreeNode): string[] =>
    node.permission ? [node.permission.key] : node.children.flatMap(keysOf)

  const item = (id: string, label: React.ReactNode, keys: string[], nested = false) => {
    const count = keys.filter((k) => value.includes(k)).length
    const checked = keys.length > 0 && count === keys.length
    return (
      <Field key={id} orientation="horizontal" className={nested ? "pl-6" : undefined}>
        <Checkbox
          id={id}
          checked={checked}
          indeterminate={!checked && count > 0}
          disabled={disabled}
          onCheckedChange={(next) => toggle(keys, next === true)}
        />
        <FieldLabel htmlFor={id} className="font-normal">
          {label}
        </FieldLabel>
      </Field>
    )
  }

  const label = (node: PermissionTreeNode) => (
    <span className="flex flex-wrap items-center gap-x-2">
      <span>{node.name}</span>
      {!node.enabled && <span className="text-xs text-muted-foreground">{t("menuDisabled")}</span>}
      {node.permission && <span className="font-mono text-xs text-muted-foreground">{node.permission.key}</span>}
    </span>
  )

  return (
    <FieldSet>
      <FieldLegend variant="label">{t("menusToUse")}</FieldLegend>
      <p className="text-sm text-muted-foreground">{t("permissionTreeHint")}</p>
      <div className="flex flex-col gap-2">
        {tree.menus.map((node) => [
          item(`menu-${node.menuId}`, label(node), keysOf(node)),
          ...node.children.map((child) => item(`menu-${child.menuId}`, label(child), keysOf(child), true)),
        ])}
      </div>
      {tree.unlisted.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium text-muted-foreground">{t("unlistedScreens")}</p>
          {tree.unlisted.map((screen) =>
            item(
              `screen-${screen.screenKey}`,
              <span className="flex flex-wrap items-center gap-x-2">
                <span>{screen.name}</span>
                <span className="font-mono text-xs text-muted-foreground">{screen.permission.key}</span>
              </span>,
              [screen.permission.key],
            ),
          )}
        </div>
      )}
    </FieldSet>
  )
}
