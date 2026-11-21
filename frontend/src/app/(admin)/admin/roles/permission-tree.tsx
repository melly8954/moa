"use client"

import { ChevronRight } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { cn } from "@/lib/utils"
import type { PermissionTree, PermissionTreeNode } from "@/types/role"

const UNLISTED = "unlisted"

// 역할 창의 권한 고르기 (moa, specs/003-admin-roles FR-004, research R10·R27)
//   관리 메뉴 트리를 체크한다. 체크한 메뉴의 연결 화면 권한이 역할에 담긴다 (value는 권한 키 목록)
//   같은 권한을 쓰는 메뉴는 함께 체크된다. 묶음 메뉴를 체크하면 하위 메뉴가 모두 체크된다
//   어느 메뉴도 가리키지 않는 관리 연결 화면은 맨 아래 "메뉴에 없는 화면"에 둔다
//   disabled면 보기만 한다 (최고 관리자 역할, 내가 다룰 수 없는 역할. specs/003-admin-roles FR-010, research R19)
//   상위 메뉴 줄은 체크 칸(아래 메뉴 모두 체크)과 접기 단추(화살표·이름·체크 수)로 나뉜다. 처음엔 첫 상위 메뉴만 펼친다
//   펼침은 기억하지 않는다. 역할을 바꾸면 부르는 쪽이 key를 바꿔 처음 모양으로 돌아온다 (2026-11-22, R27)
// 높이 (R28): 트리 목록만 칸 안에서 스크롤한다 (제목·안내는 밖). 기본은 표 머리 + 10줄 높이까지
//   fill이면 넓은 폭(xl)에서 부모 높이(옆 구성원 칸과 같은 grid 행)를 채우고, 트리 내용은 그 높이에 끼지 않는다
const LIST_MAX = "max-h-[calc(var(--table-row-height)*11)] overflow-y-auto"

export function PermissionTreePicker({
  tree,
  value,
  onChange,
  disabled = false,
  fill = false,
}: {
  tree: PermissionTree
  value: string[]
  onChange: (value: string[]) => void
  disabled?: boolean
  fill?: boolean
}) {
  const t = useTranslations("role")
  const groups = [...tree.menus.filter((node) => node.children.length > 0).map((node) => `menu-${node.menuId}`)]
  if (tree.unlisted.length > 0) groups.push(UNLISTED)
  const [open, setOpen] = useState<string[]>(groups.slice(0, 1))
  const toggleOpen = (id: string) => setOpen((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]))

  const toggle = (keys: string[], checked: boolean) =>
    onChange(checked ? [...new Set([...value, ...keys])] : value.filter((k) => !keys.includes(k)))

  const keysOf = (node: PermissionTreeNode): string[] =>
    node.permission ? [node.permission.key] : node.children.flatMap(keysOf)
  const allChecked = (keys: string[]) => keys.length > 0 && keys.every((k) => value.includes(k))

  const checkbox = (id: string, keys: string[], ariaLabel?: string) => {
    const count = keys.filter((k) => value.includes(k)).length
    const checked = allChecked(keys)
    return (
      <Checkbox
        id={id}
        aria-label={ariaLabel}
        checked={checked}
        indeterminate={!checked && count > 0}
        disabled={disabled}
        onCheckedChange={(next) => toggle(keys, next === true)}
      />
    )
  }

  const item = (id: string, label: React.ReactNode, keys: string[], nested = false) => (
    <Field key={id} orientation="horizontal" className={nested ? "pl-6" : undefined}>
      {checkbox(id, keys)}
      <FieldLabel htmlFor={id} className="font-normal">
        {label}
      </FieldLabel>
    </Field>
  )

  // 상위 메뉴 줄: 체크 칸 + 접기 단추. 접힌 줄에도 아래 메뉴 중 체크한 수/전체 수를 보인다
  const group = (
    id: string,
    name: string,
    label: React.ReactNode,
    keys: string[] | undefined,
    children: { checked: boolean; node: React.ReactNode }[],
  ) => {
    const expanded = open.includes(id)
    const panelId = `${id}-children`
    const checkedCount = children.filter((child) => child.checked).length
    return (
      <div key={id} className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          {keys ? checkbox(id, keys, t("selectAllOf", { name })) : null}
          <button
            type="button"
            aria-expanded={expanded}
            aria-controls={panelId}
            onClick={() => toggleOpen(id)}
            className="flex min-w-0 flex-1 items-center gap-1 rounded-sm text-left text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <ChevronRight aria-hidden className={cn("size-4 shrink-0 transition-transform", expanded && "rotate-90")} />
            <span className="min-w-0 flex-1">{label}</span>
            <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
              {checkedCount}/{children.length}
            </span>
          </button>
        </div>
        {expanded && (
          <div id={panelId} className="flex flex-col gap-2">
            {children.map((child) => child.node)}
          </div>
        )}
      </div>
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
    <FieldSet className={cn(fill && "xl:min-h-0 xl:flex-1")}>
      <FieldLegend variant="label">{t("menusToUse")}</FieldLegend>
      <p className="text-sm text-muted-foreground">{t("permissionTreeHint")}</p>
      <div
        className={cn(
          "rounded-lg border",
          LIST_MAX,
          fill && "relative xl:max-h-none xl:min-h-0 xl:flex-1 xl:overflow-visible",
        )}
      >
        <div className={cn("flex flex-col gap-2 p-3", fill && "xl:absolute xl:inset-0 xl:overflow-y-auto")}>
          {tree.menus.map((node) =>
            node.children.length === 0
              ? item(`menu-${node.menuId}`, label(node), keysOf(node))
              : group(
                  `menu-${node.menuId}`,
                  node.name,
                  label(node),
                  keysOf(node),
                  node.children.map((child) => ({
                    checked: allChecked(keysOf(child)),
                    node: item(`menu-${child.menuId}`, label(child), keysOf(child), true),
                  })),
                ),
          )}
          {tree.unlisted.length > 0 &&
            group(
              UNLISTED,
              t("unlistedScreens"),
              <span className="text-muted-foreground">{t("unlistedScreens")}</span>,
              undefined,
              tree.unlisted.map((screen) => ({
                checked: value.includes(screen.permission.key),
                node: item(
                  `screen-${screen.screenKey}`,
                  <span className="flex flex-wrap items-center gap-x-2">
                    <span>{screen.name}</span>
                    <span className="font-mono text-xs text-muted-foreground">{screen.permission.key}</span>
                  </span>,
                  [screen.permission.key],
                  true,
                ),
              })),
            )}
        </div>
      </div>
    </FieldSet>
  )
}
