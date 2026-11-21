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
import { useTranslations } from "next-intl"
import { DragHandle } from "@/components/kit/drag-handle"
import { cn } from "@/lib/utils"
import type { CodeGroupTree } from "@/types/code"

// 코드 그룹 목록 (moa, specs/004-admin-codes FR-005, US1-AC1·AC5·AC9, 2026-11-21). 코드 관리 왼쪽 칸
//   줄마다 끌기 손잡이, 이름, 그룹 코드(작게), 항목 수. 꺼진 그룹은 흐리게 "사용 안 함"을 붙인다
//   줄을 누르면 그 그룹을 고른다(오른쪽 칸이 바뀐다). 손잡이로 그룹 순서를 바꾼다(키보드도)
export function GroupList({
  groups,
  selected,
  onSelect,
  onDrop,
  disabled,
}: {
  groups: CodeGroupTree[]
  selected?: string
  onSelect: (groupCode: string) => void
  onDrop: (activeGroupCode: string, overGroupCode: string) => void
  disabled: boolean
}) {
  const t = useTranslations("code")
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )
  const nameOf = (id: string | number) => groups.find((g) => g.groupCode === id)?.name ?? String(id)
  const end = (event: DragEndEvent) => {
    if (event.over && event.active.id !== event.over.id) onDrop(String(event.active.id), String(event.over.id))
  }
  return (
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
      <SortableContext items={groups.map((g) => g.groupCode)} strategy={verticalListSortingStrategy}>
        <ul aria-label={t("groupListLabel")} className="flex flex-col gap-1 p-1">
          {groups.map((group) => (
            <SortableGroup
              key={group.groupCode}
              group={group}
              active={group.groupCode === selected}
              onSelect={() => onSelect(group.groupCode)}
              disabled={disabled}
            />
          ))}
        </ul>
      </SortableContext>
    </DndContext>
  )
}

function SortableGroup({
  group,
  active,
  onSelect,
  disabled,
}: {
  group: CodeGroupTree
  active: boolean
  onSelect: () => void
  disabled: boolean
}) {
  const t = useTranslations("code")
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: group.groupCode,
    disabled,
  })
  // 작은 보조 글자: 고른 줄의 배경 위에서는 대비(4.5:1)가 모자라 더 진하게 쓴다
  const sub = active ? "text-foreground/70" : "text-muted-foreground"
  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "flex items-center gap-1 rounded-md pr-2",
        active ? "bg-accent shadow-[inset_3px_0_0_var(--color-primary)]" : "hover:bg-muted/60",
        isDragging && "relative z-10 bg-muted",
      )}
    >
      <DragHandle
        activatorRef={setActivatorNodeRef}
        attributes={attributes}
        listeners={listeners}
        label={t("dragOf", { name: group.name })}
      />
      <button
        type="button"
        aria-current={active ? "true" : undefined}
        className="flex min-w-0 flex-1 items-center gap-2 py-2 text-left"
        onClick={onSelect}
      >
        <span className="flex min-w-0 flex-1 flex-col">
          <span className={cn("truncate text-sm font-medium", !group.enabled && sub)}>{group.name}</span>
          <span className={cn("truncate font-mono text-xs", sub)}>{group.groupCode}</span>
        </span>
        {!group.enabled && <span className={cn("shrink-0 text-xs", sub)}>{t("notInUse")}</span>}
        <span className={cn("shrink-0 text-xs tabular-nums", sub)}>
          {t("itemCount", { count: group.codes.length })}
        </span>
      </button>
    </li>
  )
}
