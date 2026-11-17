"use client"

import type { DraggableAttributes, DraggableSyntheticListeners } from "@dnd-kit/core"
import { GripVertical } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

// 끌기 손잡이 (moa, specs/001-admin-menus FR-010, specs/004-admin-codes FR-005). 트리 표의 행 순서를 바꾼다
//   const { attributes, listeners, setActivatorNodeRef } = useSortable({ id })
//   <DragHandle activatorRef={setActivatorNodeRef} attributes={attributes} listeners={listeners} label={…} />
//   키보드: 스페이스로 집고 화살표로 옮기고 스페이스로 놓는다
// 공용 Button은 스페이스 키를 먼저 처리해 끌기 키보드 센서가 받지 못한다. 손잡이는 맨 button으로 둔다
export function DragHandle({
  activatorRef,
  attributes,
  listeners,
  label,
}: {
  activatorRef: (element: HTMLElement | null) => void
  attributes: DraggableAttributes
  listeners: DraggableSyntheticListeners
  label: string
}) {
  return (
    <button
      ref={activatorRef}
      type="button"
      className={cn(buttonVariants({ variant: "ghost", size: "icon-sm" }), "cursor-grab touch-none")}
      aria-label={label}
      {...attributes}
      {...listeners}
    >
      <GripVertical />
    </button>
  )
}
