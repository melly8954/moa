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
import { StatusText } from "@/components/kit/status-text"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { Code, CodeGroupTree } from "@/types/code"

// 공통코드 트리 표 (moa, specs/004-admin-codes FR-005, US1, research R7). 메뉴 관리 트리 표와 같은 틀이다
//   구성 목록이라 쪽 나눔 없이 전체를 그린다. 그룹 행 아래에 그 그룹의 항목을 들여 쓴다
//   그룹 행: 접고 펴기, 이름·그룹 코드·쓰는 곳. 체크 칸은 없다 (그룹은 코드에 등록한다)
//   항목 행: 체크박스(일괄 처리), 손잡이(같은 그룹 안 순서), 이름(누르면 드로어), 코드, 사용
//   그룹마다 맨 아래에 "항목 추가" 줄

// 한 표에 여러 그룹의 항목이 있어 끌기·고르기 키는 "그룹 코드/코드"로 쓴다 (코드에는 /가 없다)
export const keyOf = (groupCode: string, code: string) => `${groupCode}/${code}`
export const parseKey = (key: string) => {
  const at = key.indexOf("/")
  return { groupCode: key.slice(0, at), code: key.slice(at + 1) }
}

export function CodeTreeTable({
  tree,
  selected,
  onSelectedChange,
  onOpen,
  onAdd,
  onDrop,
  disabled,
}: {
  tree: CodeGroupTree[]
  selected: string[]
  onSelectedChange: (selected: string[]) => void
  onOpen: (group: CodeGroupTree, code: Code) => void
  onAdd: (group: CodeGroupTree) => void
  // 다른 그룹 위에 놓으면 activeKey와 overKey의 그룹이 다르다. 부르는 쪽이 안내한다
  onDrop: (activeKey: string, overKey: string) => void
  disabled: boolean
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
  const toggleGroup = (groupCode: string) =>
    setCollapsed(collapsed.includes(groupCode) ? collapsed.filter((g) => g !== groupCode) : [...collapsed, groupCode])

  const end = (event: DragEndEvent) => {
    if (event.over && event.active.id !== event.over.id) onDrop(String(event.active.id), String(event.over.id))
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
                  <TableRow className="bg-muted/50 hover:bg-muted/50">
                    <TableCell colSpan={5}>
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                        <button
                          type="button"
                          className="flex items-center gap-1 font-medium"
                          aria-expanded={open}
                          aria-label={t(open ? "collapse" : "expand", { name: group.name })}
                          onClick={() => toggleGroup(group.groupCode)}
                        >
                          <Chevron aria-hidden="true" className="size-4" />
                          {group.name}
                        </button>
                        <span className="font-mono text-xs">{group.groupCode}</span>
                        <span className="text-xs text-muted-foreground">{t("usedBy", { usedBy: group.usedBy })}</span>
                      </div>
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
                              onCheck={(checked) => toggle(key, checked)}
                              onOpen={() => onOpen(group, code)}
                              disabled={disabled}
                            />
                          )
                        })}
                      </SortableContext>
                      {group.codes.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={2} />
                          <TableCell colSpan={3} className="pl-8 text-muted-foreground">
                            {t("noCodes")}
                          </TableCell>
                        </TableRow>
                      )}
                      <TableRow className="hover:bg-transparent">
                        <TableCell colSpan={2} />
                        <TableCell colSpan={3} className="pl-6">
                          <Button
                            variant="ghost"
                            size="sm"
                            aria-label={t("addCodeOf", { name: group.name })}
                            onClick={() => onAdd(group)}
                          >
                            <Plus />
                            {t("addCode")}
                          </Button>
                        </TableCell>
                      </TableRow>
                    </>
                  )}
                </Fragment>
              )
            })}
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
  onCheck,
  onOpen,
  disabled,
}: {
  id: string
  code: Code
  checked: boolean
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
      className={cn(isDragging && "relative z-10 bg-muted")}
      data-state={checked ? "selected" : undefined}
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
