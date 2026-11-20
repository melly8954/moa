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
import { ChevronDown, ChevronRight, Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { Fragment, useState } from "react"
import { DragHandle } from "@/components/kit/drag-handle"
import { openOnRowClick, ROW_CLICKABLE } from "@/components/kit/row-click"
import { StatusText } from "@/components/kit/status-text"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { Code, CodeGroupTree } from "@/types/code"

// 공통코드 트리 표 (moa, specs/004-admin-codes FR-005, US1, research R7). 메뉴 관리 트리 표와 같은 틀이다
//   구성 목록이라 쪽 나눔 없이 전체를 그린다. 그룹 행 아래에 그 그룹의 항목을 들여 쓴다
//   그룹·항목 행은 어디를 눌러도 오른쪽 패널이 열린다 (2026-11-21). 체크박스·접기 단추·손잡이는 제 일만 하고, 키보드는 이름 단추로 연다
//   사용 안 함은 회색(neutral)이다. 빨강은 문제 표시에만 쓴다
//   그룹 행도 항목 행과 같은 칸을 쓴다 (메뉴 관리 트리 표와 같다): 첫 칸에 항목 전체 고르기, 둘째 칸에 접고 펴기, 이름 칸에 이름(누르면 그룹 드로어)·
//   설명(꺼진 그룹은 이름을 흐리게), 코드 칸에 그룹 코드, 사용 칸에 상태 (FR-005·FR-008)
//   항목 행: 체크박스(일괄 처리), 손잡이(같은 그룹 안 순서), 이름(누르면 드로어), 코드, 사용
//   그룹마다 맨 아래에 "항목 추가" 줄

// 한 표에 여러 그룹의 항목이 있어 끌기·고르기 키는 "그룹 코드/코드"로 쓴다 (코드에는 /가 없다)
// 일부만 고른 그룹: 채운 칸에 "−" (공통 Checkbox는 일부 선택에도 ✓를 그려 구분이 어렵다. ui는 생성 코드라 여기서 입힌다)
const INDETERMINATE =
  "data-indeterminate:border-primary data-indeterminate:bg-primary data-indeterminate:text-primary-foreground data-indeterminate:[&_svg]:hidden data-indeterminate:before:h-0.5 data-indeterminate:before:w-2 data-indeterminate:before:rounded-full data-indeterminate:before:bg-current data-indeterminate:before:content-['']"

// 오른쪽 패널에 열린 행: 배경과 첫 칸 왼쪽의 막대로 보인다
const ACTIVE_ROW = "bg-accent hover:bg-accent [&>td:first-child]:shadow-[inset_3px_0_0_var(--color-primary)]"

export const keyOf = (groupCode: string, code: string) => `${groupCode}/${code}`
export const parseKey = (key: string) => {
  const at = key.indexOf("/")
  return { groupCode: key.slice(0, at), code: key.slice(at + 1) }
}

export function CodeTreeTable({
  tree,
  selected,
  onSelectedChange,
  active,
  onOpen,
  onOpenGroup,
  onAdd,
  onAddGroup,
  onDrop,
  disabled,
  className,
}: {
  tree: CodeGroupTree[]
  selected: string[]
  onSelectedChange: (selected: string[]) => void
  // 오른쪽 패널에 열린 것 ("group:그룹 코드" 또는 "그룹 코드/코드"). 그 행을 강조한다
  active?: string
  onOpen: (group: CodeGroupTree, code: Code) => void
  onOpenGroup: (group: CodeGroupTree) => void
  onAdd: (group: CodeGroupTree) => void
  // 트리 맨 아래 "그룹 추가" 줄
  onAddGroup: () => void
  // 다른 그룹 위에 놓으면 activeKey와 overKey의 그룹이 다르다. 부르는 쪽이 안내한다
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
    return tree.find((g) => g.groupCode === groupCode)?.codes.find((c) => c.code === code)?.name ?? ""
  }

  const toggle = (key: string, checked: boolean) =>
    onSelectedChange(checked ? [...selected, key] : selected.filter((s) => s !== key))
  const groupKeys = (group: CodeGroupTree) => group.codes.map((c) => keyOf(group.groupCode, c.code))
  const groupState = (group: CodeGroupTree) => {
    const keys = groupKeys(group)
    const count = keys.filter((key) => selected.includes(key)).length
    return count === 0 ? "none" : count === keys.length ? "all" : "some"
  }
  const toggleAll = (group: CodeGroupTree, checked: boolean) => {
    const keys = groupKeys(group)
    const rest = selected.filter((key) => !keys.includes(key))
    onSelectedChange(checked ? [...rest, ...keys] : rest)
  }
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
                <span className="sr-only">{t("selectColumn")}</span>
              </TableHead>
              <TableHead className="w-10">
                <span className="sr-only">{t("dragColumn")}</span>
              </TableHead>
              <TableHead>{t("name")}</TableHead>
              <TableHead>{t("code")}</TableHead>
              <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tree.map((group) => {
              const open = !collapsed.includes(group.groupCode)
              const Chevron = open ? ChevronDown : ChevronRight
              return (
                <Fragment key={group.groupCode}>
                  <TableRow
                    className={cn(
                      ROW_CLICKABLE,
                      "bg-muted/50 hover:bg-muted/50",
                      active === `group:${group.groupCode}` && ACTIVE_ROW,
                    )}
                    aria-current={active === `group:${group.groupCode}` ? "true" : undefined}
                    onClick={openOnRowClick(() => onOpenGroup(group))}
                  >
                    <TableCell>
                      {/* 그룹 체크 = 그 그룹 항목 전부 고르기·풀기. 처리되는 것은 항목뿐이다 (그룹 삭제는 패널에서 하나씩). 항목이 없으면 체크 칸을 두지 않는다 */}
                      {group.codes.length > 0 && (
                        <Checkbox
                          aria-label={t("selectAllOf", { name: group.name })}
                          checked={groupState(group) === "all"}
                          indeterminate={groupState(group) === "some"}
                          className={INDETERMINATE}
                          onCheckedChange={(c) => toggleAll(group, c === true)}
                        />
                      )}
                    </TableCell>
                    <TableCell>
                      <button
                        type="button"
                        className="flex size-4 items-center justify-center rounded-sm"
                        aria-expanded={open}
                        aria-label={t(open ? "collapse" : "expand", { name: group.name })}
                        onClick={() => toggleGroup(group.groupCode)}
                      >
                        <Chevron aria-hidden="true" className="size-4" />
                      </button>
                    </TableCell>
                    <TableCell>
                      <div className="flex max-w-(--col-text-max) flex-col">
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            className={cn(
                              "truncate text-left font-medium underline-offset-4 hover:underline",
                              !group.enabled && "text-muted-foreground",
                            )}
                            aria-label={t("editGroupOf", { name: group.name })}
                            onClick={() => onOpenGroup(group)}
                          >
                            {group.name}
                          </button>
                        </div>
                        {group.description && (
                          <span className="truncate text-xs text-muted-foreground">{group.description}</span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono">{group.groupCode}</span>
                    </TableCell>
                    <TableCell>
                      <StatusText tone={group.enabled ? "positive" : "neutral"}>
                        {t(group.enabled ? "inUse" : "notInUse")}
                      </StatusText>
                    </TableCell>
                  </TableRow>
                  {open && (
                    <>
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
                      {group.codes.length === 0 && (
                        <TableRow className="hover:bg-transparent">
                          <TableCell colSpan={2} />
                          <TableCell colSpan={3}>
                            <span className="pl-6 text-muted-foreground">{t("noCodes")}</span>
                          </TableCell>
                        </TableRow>
                      )}
                      <TableRow className="hover:bg-transparent">
                        <TableCell colSpan={2} />
                        <TableCell colSpan={3}>
                          <div className="pl-6">
                            <button
                              type="button"
                              className="flex items-center gap-1 text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
                              aria-label={t("addCodeOf", { name: group.name })}
                              onClick={() => onAdd(group)}
                            >
                              <Plus aria-hidden="true" className="size-4" />
                              {t("addCode")}
                            </button>
                          </div>
                        </TableCell>
                      </TableRow>
                    </>
                  )}
                </Fragment>
              )
            })}
            <TableRow className="hover:bg-transparent">
              <TableCell colSpan={2} />
              <TableCell colSpan={3}>
                <button
                  type="button"
                  className="flex items-center gap-1 text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
                  onClick={onAddGroup}
                >
                  <Plus aria-hidden="true" className="size-4" />
                  {t("addGroup")}
                </button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </DndContext>
    </div>
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
        <div className="max-w-(--col-text-max) truncate pl-6">
          <button type="button" className="text-left underline-offset-4 hover:underline" onClick={onOpen}>
            {code.name}
          </button>
        </div>
      </TableCell>
      <TableCell>
        <span className="font-mono">{code.code}</span>
      </TableCell>
      <TableCell>
        <StatusText tone={code.enabled ? "positive" : "neutral"}>{t(code.enabled ? "inUse" : "notInUse")}</StatusText>
      </TableCell>
    </TableRow>
  )
}
