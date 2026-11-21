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
import { ChevronDown, ChevronUp, FileText, Folder } from "lucide-react"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { DragHandle } from "@/components/kit/drag-handle"
import { MenuIcon } from "@/components/menu/icons"
import { openOnRowClick, ROW_CLICKABLE } from "@/components/kit/row-click"
import { StatusText } from "@/components/kit/status-text"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { AdminMenu } from "@/types/menu"
import { isGroup, type MenuRow, rowsOf } from "./menu-order"

// 메뉴 트리 표 (moa, specs/001-admin-menus FR-010, US5, research R20·R22·R25)
//   구성 목록이라 쪽 나눔 없이 전체를 그린다. 상위 메뉴 바로 아래에 하위 메뉴를 들여 쓴다
//   행 단추는 없다: 체크박스(일괄 처리), 손잡이(끌어서 순서·부모), 메뉴명. 행 어디를 눌러도 오른쪽 상세 칸이 열린다
//   (체크박스·손잡이는 제 일만 한다. 키보드는 메뉴명 단추로 연다)
//   열은 체크박스·손잡이·메뉴명·사용이다. 연결 화면과 누가 보는지(권한·공개 범위)는 상세 칸에 보인다 (2026-11-21)
//   메뉴명 앞에 메뉴의 아이콘(없으면 상위 메뉴는 폴더, 화면 메뉴는 문서). 사용 안 함은 회색(neutral), 빨강은 "없는 화면"에만 쓴다
//   상세 칸에 열린 행(active)은 강조한다
//   손잡이는 키보드로도 옮긴다 (스페이스로 집고 화살표로 옮기고 스페이스로 놓기)
//   하위 메뉴가 있는 상위 메뉴는 메뉴명 칸 끝의 "N개" 단추로 접고 편다. 이 단추는 접기만 하고 상세를 열지 않는다 (2026-11-21)
//   접힌 하위 메뉴는 그리지 않고 끌기 대상에서도 빠진다. 고른 상태는 그대로다
export function MenuTreeTable({
  tree,
  selected,
  onSelectedChange,
  onOpen,
  onDrop,
  disabled,
  active,
  className,
}: {
  tree: AdminMenu[]
  selected: number[]
  onSelectedChange: (selected: number[]) => void
  onOpen: (menu: AdminMenu) => void
  onDrop: (activeId: number, overId: number) => void
  disabled: boolean
  active?: number
  className?: string
}) {
  const t = useTranslations("menuAdmin")
  const allRows = rowsOf(tree)
  // 접은 상위 메뉴 id. 처음에는 모두 펼친다
  const [collapsed, setCollapsed] = useState<number[]>([])
  const rows = allRows.filter((r) => r.depth === 0 || !collapsed.includes(r.menu.parentId ?? -1))
  const toggleOpen = (id: number) =>
    setCollapsed(collapsed.includes(id) ? collapsed.filter((c) => c !== id) : [...collapsed, id])
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )
  const allIds = allRows.map((r) => r.menu.id)
  const allChecked = allIds.length > 0 && allIds.every((id) => selected.includes(id))
  const someChecked = !allChecked && allIds.some((id) => selected.includes(id))

  const toggle = (id: number, checked: boolean) =>
    onSelectedChange(checked ? [...selected, id] : selected.filter((s) => s !== id))

  const end = (event: DragEndEvent) => {
    if (event.over && event.active.id !== event.over.id) onDrop(Number(event.active.id), Number(event.over.id))
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
            onDragStart: ({ active }) => t("dragStart", { name: nameOf(rows, active.id) }),
            onDragOver: ({ active, over }) =>
              over ? t("dragOver", { name: nameOf(rows, active.id), over: nameOf(rows, over.id) }) : "",
            onDragEnd: ({ active, over }) =>
              over ? t("dragEnd", { name: nameOf(rows, active.id), over: nameOf(rows, over.id) }) : "",
            onDragCancel: ({ active }) => t("dragCancel", { name: nameOf(rows, active.id) }),
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
                  onCheckedChange={(checked) => onSelectedChange(checked === true ? allIds : [])}
                />
              </TableHead>
              <TableHead className="w-10">
                <span className="sr-only">{t("dragColumn")}</span>
              </TableHead>
              <TableHead>{t("name")}</TableHead>
              <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <SortableContext items={rows.map((r) => r.menu.id)} strategy={verticalListSortingStrategy}>
              {rows.map((row) => (
                <SortableMenuRow
                  key={row.menu.id}
                  row={row}
                  checked={selected.includes(row.menu.id)}
                  onCheck={(checked) => toggle(row.menu.id, checked)}
                  onOpen={() => onOpen(row.menu)}
                  disabled={disabled}
                  active={active === row.menu.id}
                  open={!collapsed.includes(row.menu.id)}
                  onToggleOpen={() => toggleOpen(row.menu.id)}
                />
              ))}
            </SortableContext>
          </TableBody>
        </Table>
      </DndContext>
    </div>
  )
}

function nameOf(rows: MenuRow[], id: string | number): string {
  return rows.find((r) => r.menu.id === Number(id))?.menu.name ?? ""
}

function SortableMenuRow({
  row,
  checked,
  onCheck,
  onOpen,
  disabled,
  active,
  open,
  onToggleOpen,
}: {
  row: MenuRow
  checked: boolean
  onCheck: (checked: boolean) => void
  onOpen: () => void
  disabled: boolean
  active: boolean
  // 하위 메뉴가 펼쳐져 있는가 (하위 메뉴가 있는 상위 메뉴만)
  open: boolean
  onToggleOpen: () => void
}) {
  const t = useTranslations("menuAdmin")
  const { menu, depth } = row
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: menu.id,
    disabled,
  })
  const group = isGroup(menu)

  return (
    <TableRow
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(ROW_CLICKABLE, active && "bg-muted", isDragging && "relative z-10 bg-muted")}
      aria-current={active ? "true" : undefined}
      data-state={checked ? "selected" : undefined}
      onClick={openOnRowClick(onOpen)}
    >
      <TableCell>
        <Checkbox
          aria-label={t("selectOf", { name: menu.name })}
          checked={checked}
          onCheckedChange={(c) => onCheck(c === true)}
        />
      </TableCell>
      <TableCell>
        <DragHandle
          activatorRef={setActivatorNodeRef}
          attributes={attributes}
          listeners={listeners}
          label={t("dragOf", { name: menu.name })}
        />
      </TableCell>
      <TableCell>
        <div className={cn("flex min-w-0 items-center gap-2", depth === 1 && "pl-6")}>
          {/* 메뉴의 아이콘. 없으면 상위 메뉴는 폴더, 화면 메뉴는 문서 (FR-010, 2026-11-21) */}
          <MenuIcon
            name={menu.icon}
            fallback={group ? Folder : FileText}
            aria-hidden="true"
            className="size-4 shrink-0 text-muted-foreground"
          />
          <button type="button" className={cn("text-left", group && "font-medium")} onClick={onOpen}>
            {menu.name}
          </button>
          {menu.missingScreen && <StatusText tone="negative">{t("missingScreen")}</StatusText>}
          {/* 하위 메뉴 접기·펴기. 글자가 있는 단추라 상세 열기(행 누르기)와 나뉜다 (FR-010, 2026-11-21) */}
          {depth === 0 && menu.children.length > 0 && (
            <button
              type="button"
              aria-expanded={open}
              aria-label={t(open ? "collapseChildren" : "expandChildren", {
                name: menu.name,
                count: menu.children.length,
              })}
              className="ml-auto inline-flex shrink-0 items-center gap-0.5 rounded-full border px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted hover:text-foreground"
              onClick={onToggleOpen}
            >
              {t("childCount", { count: menu.children.length })}
              {open ? (
                <ChevronUp aria-hidden="true" className="size-3.5" />
              ) : (
                <ChevronDown aria-hidden="true" className="size-3.5" />
              )}
            </button>
          )}
        </div>
      </TableCell>
      <TableCell>
        <StatusText tone={menu.enabled ? "positive" : "neutral"}>{t(menu.enabled ? "inUse" : "notInUse")}</StatusText>
      </TableCell>
    </TableRow>
  )
}
