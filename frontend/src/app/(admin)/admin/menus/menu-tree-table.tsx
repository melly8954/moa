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
import { GripVertical, type LucideIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import { StatusText } from "@/components/kit/status-text"
import { menuIcon } from "@/components/menu/icons"
import { buttonVariants } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { AdminMenu } from "@/types/menu"
import { isGroup, type MenuRow, rowsOf } from "./menu-order"

// 메뉴 트리 표 (moa, specs/001-admin-menus FR-010, US5, research R20·R22)
//   구성 목록이라 쪽 나눔 없이 전체를 그린다. 묶음 바로 아래에 하위 메뉴를 들여 쓴다
//   행 단추는 없다: 체크박스(일괄 처리), 손잡이(끌어서 순서·부모), 이름(누르면 드로어)
//   손잡이는 키보드로도 옮긴다 (스페이스로 집고 화살표로 옮기고 스페이스로 놓기)
export function MenuTreeTable({
  tree,
  selected,
  onSelectedChange,
  onOpen,
  onDrop,
  disabled,
}: {
  tree: AdminMenu[]
  selected: number[]
  onSelectedChange: (selected: number[]) => void
  onOpen: (menu: AdminMenu) => void
  onDrop: (activeId: number, overId: number) => void
  disabled: boolean
}) {
  const t = useTranslations("menuAdmin")
  const rows = rowsOf(tree)
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )
  const allIds = rows.map((r) => r.menu.id)
  const allChecked = allIds.length > 0 && allIds.every((id) => selected.includes(id))
  const someChecked = !allChecked && allIds.some((id) => selected.includes(id))

  const toggle = (id: number, checked: boolean) =>
    onSelectedChange(checked ? [...selected, id] : selected.filter((s) => s !== id))

  const end = (event: DragEndEvent) => {
    if (event.over && event.active.id !== event.over.id) onDrop(Number(event.active.id), Number(event.over.id))
  }

  return (
    <div className="rounded-lg border">
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
              <TableHead>{t("program")}</TableHead>
              <TableHead>{t("whoSees")}</TableHead>
              <TableHead className="w-(--col-status)">{t("enabled")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <SortableContext items={allIds} strategy={verticalListSortingStrategy}>
              {rows.map((row) => (
                <SortableMenuRow
                  key={row.menu.id}
                  row={row}
                  icon={menuIcon(row.menu.icon)}
                  checked={selected.includes(row.menu.id)}
                  onCheck={(checked) => toggle(row.menu.id, checked)}
                  onOpen={() => onOpen(row.menu)}
                  disabled={disabled}
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
  icon: Icon,
  checked,
  onCheck,
  onOpen,
  disabled,
}: {
  row: MenuRow
  icon?: LucideIcon
  checked: boolean
  onCheck: (checked: boolean) => void
  onOpen: () => void
  disabled: boolean
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
      className={cn(isDragging && "relative z-10 bg-muted")}
      data-state={checked ? "selected" : undefined}
    >
      <TableCell>
        <Checkbox
          aria-label={t("selectOf", { name: menu.name })}
          checked={checked}
          onCheckedChange={(c) => onCheck(c === true)}
        />
      </TableCell>
      <TableCell>
        {/* 공용 Button은 스페이스 키를 먼저 처리해 끌기 키보드 센서가 받지 못한다. 손잡이는 맨 button으로 둔다 */}
        <button
          ref={setActivatorNodeRef}
          type="button"
          className={cn(buttonVariants({ variant: "ghost", size: "icon-sm" }), "cursor-grab touch-none")}
          aria-label={t("dragOf", { name: menu.name })}
          {...attributes}
          {...listeners}
        >
          <GripVertical />
        </button>
      </TableCell>
      <TableCell>
        <div className={cn("flex items-center gap-2", depth === 1 && "pl-6")}>
          {Icon && <Icon aria-hidden="true" className="size-4 text-muted-foreground" />}
          <button
            type="button"
            className={cn("text-left underline-offset-4 hover:underline", group && "font-medium")}
            onClick={onOpen}
          >
            {menu.name}
          </button>
        </div>
      </TableCell>
      <TableCell>
        {menu.program ? (
          <div className="flex flex-col">
            <span>{menu.program.name}</span>
            <span className="font-mono text-xs text-muted-foreground">{menu.program.path}</span>
          </div>
        ) : menu.missingProgram ? (
          <StatusText tone="negative">{t("missingProgram")}</StatusText>
        ) : (
          <span className="text-muted-foreground">{t("group")}</span>
        )}
      </TableCell>
      <TableCell>
        <WhoSees menu={menu} />
      </TableCell>
      <TableCell>
        <StatusText tone={menu.enabled ? "positive" : "neutral"}>{t(menu.enabled ? "inUse" : "notInUse")}</StatusText>
      </TableCell>
    </TableRow>
  )
}

// 사이드바에 누구에게 보이나. 관리 메뉴는 프로그램의 권한, 사용자 메뉴는 공개 범위, 묶음 메뉴는 하위 메뉴를 따른다
function WhoSees({ menu }: { menu: AdminMenu }) {
  const t = useTranslations("menuAdmin")
  if (!menu.programKey) return <span className="text-muted-foreground">{t("followsChildren")}</span>
  if (!menu.program) return <span className="text-muted-foreground">{t("hidden")}</span>
  if (menu.audience === "ADMIN") return <>{menu.program.permissionName}</>
  return <>{t(menu.visibility === "MEMBER" ? "visibilityMember" : "visibilityPublic")}</>
}
