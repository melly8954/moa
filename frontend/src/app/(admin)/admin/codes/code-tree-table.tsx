"use client"

import {
  closestCenter,
  DndContext,
  type DragEndEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core"
import {
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { ChevronDown, ChevronRight, FileText, Folder } from "lucide-react"
import { useTranslations } from "next-intl"
import { Fragment, useState } from "react"
import { DragHandle } from "@/components/kit/drag-handle"
import { openOnRowClick, ROW_CLICKABLE } from "@/components/kit/row-click"
import { StatusText } from "@/components/kit/status-text"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { Code, CodeGroupTree } from "@/types/code"

// 공통코드 트리 표 (moa, specs/004-admin-codes FR-005, US1). 메뉴 관리 트리 표와 같은 조작이다 (2026-11-21)
//   구성 목록이라 쪽 나눔 없이 전체를 그린다. 그룹 행 아래에 그 그룹의 항목을 들여 쓴다
//   그룹 행과 항목 행은 같은 칸을 쓴다: 체크박스(일괄 처리), 손잡이(순서), 이름(그룹은 접기 단추·폴더 아이콘·설명, 항목은 문서 아이콘), 코드, 사용
//   그룹 행 체크는 그 그룹 자체를 고른다. 그룹은 그룹끼리, 항목은 같은 그룹 안에서 끌어 순서를 바꾼다
//   행 어디를 눌러도 오른쪽 패널이 열린다 (체크박스·손잡이·접기 단추는 제 일만 한다. 키보드는 이름 단추로 연다)
//   새 그룹·항목은 트리 위 "추가"로 만든다 (트리 안에 추가 줄을 두지 않는다). 사용 안 함은 회색(neutral)

// 끌기·고르기 키: 그룹은 "group:그룹 코드", 항목은 "그룹 코드/코드" (코드에는 /와 :가 없다)
export const keyOf = (groupCode: string, code: string) => `${groupCode}/${code}`
export const groupKeyOf = (groupCode: string) => `group:${groupCode}`
export const parseKey = (key: string): { groupCode: string; code?: string } => {
  if (key.startsWith("group:")) return { groupCode: key.slice("group:".length) }
  const at = key.indexOf("/")
  return { groupCode: key.slice(0, at), code: key.slice(at + 1) }
}

// 코드 열은 1280px 이상에서만 둔다. 두 칸 화면이 막 시작하는 폭(1024px~)에서는 표가 칸보다 넓어져,
// 그때는 코드를 이름 아래(그룹)·옆(항목)에 작게 보인다. 긴 코드는 말줄임
const CODE_COLUMN = "hidden xl:table-cell"

// 오른쪽 패널에 열린 행: 배경과 첫 칸 왼쪽의 막대로 보인다
const ACTIVE_ROW = "bg-accent hover:bg-accent [&>td:first-child]:shadow-[inset_3px_0_0_var(--color-primary)]"

export function CodeTreeTable({
  tree,
  selected,
  onSelectedChange,
  active,
  onOpen,
  onOpenGroup,
  onDrop,
  disabled,
  className,
}: {
  tree: CodeGroupTree[]
  selected: string[]
  onSelectedChange: (selected: string[]) => void
  // 오른쪽 패널에 열린 것 (groupKeyOf 또는 keyOf). 그 행을 강조한다
  active?: string
  onOpen: (group: CodeGroupTree, code: Code) => void
  onOpenGroup: (group: CodeGroupTree) => void
  // 그룹끼리, 또는 항목끼리 놓았을 때. 다른 그룹·다른 종류면 부르는 쪽이 안내한다
  onDrop: (activeKey: string, overKey: string) => void
  disabled: boolean
  className?: string
}) {
  const t = useTranslations("code")
  const [collapsed, setCollapsed] = useState<string[]>([])
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )
  const nameOf = (key: string | number) => {
    const { groupCode, code } = parseKey(String(key))
    const group = tree.find((g) => g.groupCode === groupCode)
    return (code === undefined ? group?.name : group?.codes.find((c) => c.code === code)?.name) ?? ""
  }
  const allKeys = tree.flatMap((g) => [groupKeyOf(g.groupCode), ...g.codes.map((c) => keyOf(g.groupCode, c.code))])
  const allChecked = allKeys.length > 0 && allKeys.every((key) => selected.includes(key))
  const someChecked = !allChecked && allKeys.some((key) => selected.includes(key))

  const toggle = (key: string, checked: boolean) =>
    onSelectedChange(checked ? [...selected, key] : selected.filter((s) => s !== key))
  const toggleGroup = (groupCode: string) =>
    setCollapsed(collapsed.includes(groupCode) ? collapsed.filter((g) => g !== groupCode) : [...collapsed, groupCode])

  const end = (event: DragEndEvent) => {
    if (event.over && event.active.id !== event.over.id) onDrop(String(event.active.id), String(event.over.id))
  }

  return (
    <div className={cn("rounded-lg border", className)}>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={end}
        accessibility={{
          screenReaderInstructions: { draggable: t("dragInstructions") },
          announcements: {
            onDragStart: ({ active }) => t("dragStart", { name: nameOf(active.id) }),
            onDragOver: ({ active, over }) =>
              over ? t("dragOver", { name: nameOf(active.id), over: nameOf(over.id) }) : "",
            onDragEnd: ({ active, over }) =>
              over ? t("dragEnd", { name: nameOf(active.id), over: nameOf(over.id) }) : "",
            onDragCancel: ({ active }) => t("dragCancel", { name: nameOf(active.id) }),
          },
        }}
      >
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <Checkbox
                  aria-label={t("selectAll")}
                  checked={allChecked}
                  indeterminate={someChecked}
                  onCheckedChange={(checked) => onSelectedChange(checked === true ? allKeys : [])}
                />
              </TableHead>
              <TableHead className="w-10">
                <span className="sr-only">{t("dragColumn")}</span>
              </TableHead>
              <TableHead>{t("name")}</TableHead>
              <TableHead className={CODE_COLUMN}>{t("code")}</TableHead>
              <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <SortableContext items={tree.map((g) => groupKeyOf(g.groupCode))} strategy={verticalListSortingStrategy}>
              {tree.map((group) => {
                const open = !collapsed.includes(group.groupCode)
                const groupKey = groupKeyOf(group.groupCode)
                return (
                  <Fragment key={group.groupCode}>
                    <SortableGroupRow
                      group={group}
                      open={open}
                      onToggleOpen={() => toggleGroup(group.groupCode)}
                      checked={selected.includes(groupKey)}
                      active={active === groupKey}
                      onCheck={(checked) => toggle(groupKey, checked)}
                      onOpen={() => onOpenGroup(group)}
                      disabled={disabled}
                    />
                    {open && (
                      <SortableContext
                        items={group.codes.map((c) => keyOf(group.groupCode, c.code))}
                        strategy={verticalListSortingStrategy}
                      >
                        {group.codes.map((code) => {
                          const key = keyOf(group.groupCode, code.code)
                          return (
                            <SortableCodeRow
                              key={key}
                              id={key}
                              code={code}
                              checked={selected.includes(key)}
                              active={active === key}
                              onCheck={(checked) => toggle(key, checked)}
                              onOpen={() => onOpen(group, code)}
                              disabled={disabled}
                            />
                          )
                        })}
                      </SortableContext>
                    )}
                  </Fragment>
                )
              })}
            </SortableContext>
          </TableBody>
        </Table>
      </DndContext>
    </div>
  )
}

function SortableGroupRow({
  group,
  open,
  onToggleOpen,
  checked,
  active,
  onCheck,
  onOpen,
  disabled,
}: {
  group: CodeGroupTree
  open: boolean
  onToggleOpen: () => void
  checked: boolean
  active: boolean
  onCheck: (checked: boolean) => void
  onOpen: () => void
  disabled: boolean
}) {
  const t = useTranslations("code")
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: groupKeyOf(group.groupCode),
    disabled,
  })
  const Chevron = open ? ChevronDown : ChevronRight

  return (
    <TableRow
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        ROW_CLICKABLE,
        "bg-muted/50 hover:bg-muted/50",
        active && ACTIVE_ROW,
        isDragging && "relative z-10 bg-muted",
      )}
      aria-current={active ? "true" : undefined}
      data-state={checked ? "selected" : undefined}
      onClick={openOnRowClick(onOpen)}
    >
      <TableCell>
        <Checkbox
          aria-label={t("selectOf", { name: group.name })}
          checked={checked}
          onCheckedChange={(c) => onCheck(c === true)}
        />
      </TableCell>
      <TableCell>
        <DragHandle
          activatorRef={setActivatorNodeRef}
          attributes={attributes}
          listeners={listeners}
          label={t("dragOf", { name: group.name })}
        />
      </TableCell>
      <TableCell>
        <div className="flex max-w-(--col-text-max) items-start gap-1.5">
          <button
            type="button"
            className="mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-sm"
            aria-expanded={open}
            aria-label={t(open ? "collapse" : "expand", { name: group.name })}
            onClick={onToggleOpen}
          >
            <Chevron aria-hidden="true" className="size-4" />
          </button>
          <Folder aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
          <div className="flex min-w-0 flex-col">
            <button
              type="button"
              className={cn("truncate text-left font-medium", !group.enabled && "text-muted-foreground")}
              aria-label={t("editGroupOf", { name: group.name })}
              onClick={onOpen}
            >
              {group.name}
            </button>
            {group.description && <span className="truncate text-xs text-muted-foreground">{group.description}</span>}
            <span className="truncate font-mono text-xs text-muted-foreground xl:hidden">{group.groupCode}</span>
          </div>
        </div>
      </TableCell>
      <TableCell className={CODE_COLUMN}>
        <span className="block max-w-40 truncate font-mono" title={group.groupCode}>
          {group.groupCode}
        </span>
      </TableCell>
      <TableCell>
        <StatusText tone={group.enabled ? "positive" : "neutral"}>{t(group.enabled ? "inUse" : "notInUse")}</StatusText>
      </TableCell>
    </TableRow>
  )
}

function SortableCodeRow({
  id,
  code,
  checked,
  active,
  onCheck,
  onOpen,
  disabled,
}: {
  id: string
  code: Code
  checked: boolean
  active: boolean
  onCheck: (checked: boolean) => void
  onOpen: () => void
  disabled: boolean
}) {
  const t = useTranslations("code")
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id,
    disabled,
  })

  return (
    <TableRow
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(ROW_CLICKABLE, active && ACTIVE_ROW, isDragging && "relative z-10 bg-muted")}
      aria-current={active ? "true" : undefined}
      data-state={checked ? "selected" : undefined}
      onClick={openOnRowClick(onOpen)}
    >
      <TableCell>
        <Checkbox
          aria-label={t("selectOf", { name: code.name })}
          checked={checked}
          onCheckedChange={(c) => onCheck(c === true)}
        />
      </TableCell>
      <TableCell>
        <DragHandle
          activatorRef={setActivatorNodeRef}
          attributes={attributes}
          listeners={listeners}
          label={t("dragOf", { name: code.name })}
        />
      </TableCell>
      <TableCell>
        <div className="flex max-w-(--col-text-max) items-center gap-2 pl-[2.375rem]">
          <FileText aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
          <button type="button" className="truncate text-left" onClick={onOpen}>
            {code.name}
          </button>
          <span className="shrink-0 font-mono text-xs text-muted-foreground xl:hidden">{code.code}</span>
        </div>
      </TableCell>
      <TableCell className={CODE_COLUMN}>
        <span className="block max-w-40 truncate font-mono" title={code.code}>
          {code.code}
        </span>
      </TableCell>
      <TableCell>
        <StatusText tone={code.enabled ? "positive" : "neutral"}>{t(code.enabled ? "inUse" : "notInUse")}</StatusText>
      </TableCell>
    </TableRow>
  )
}
