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
import { Pencil, Plus } from "lucide-react"
import { useTranslations } from "next-intl"
import { type FormEvent, useState } from "react"
import { toApiError } from "@/api/error"
import { DragHandle } from "@/components/kit/drag-handle"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { FieldError } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useFieldErrorText } from "@/i18n/field-error"
import { codeItemSchema } from "@/lib/schemas/code"
import { cn } from "@/lib/utils"
import type { Code, CodeGroupTree } from "@/types/code"

// 고른 그룹의 항목 표 (moa, specs/004-admin-codes FR-005·FR-006·FR-009, US1-AC1~AC5, 2026-11-21)
//   열: 고르기, 끌기 손잡이, 코드, 이름, 사용. 구성 목록이라 쪽 나눔 없이 그린다
//   이름은 눌러 표 안에서 고친다: 엔터·칸 벗어나기는 저장, Esc는 되돌리기. 코드는 고치지 않는다
//   사용은 스위치로 바로 저장한다. 맨 아래 추가 줄에서 코드(필수)와 이름을 적어 맨 뒤에 만든다
//   항목 순서는 손잡이로 바꾼다(키보드도). 다른 그룹으로는 옮기지 않는다

// 치는 대로 대문자로, -와 빈칸은 _로, 대문자·숫자·밑줄 밖의 글자는 받지 않는다 (FR-009)
const toCode = (value: string) =>
  value
    .toUpperCase()
    .replace(/[-\s]/g, "_")
    .replace(/[^A-Z0-9_]/g, "")

export function CodeTable({
  group,
  checked,
  onCheckedChange,
  onDrop,
  onRename,
  onToggle,
  onAdd,
  disabled,
}: {
  group: CodeGroupTree
  checked: string[]
  onCheckedChange: (codes: string[]) => void
  onDrop: (activeCode: string, overCode: string) => void
  onRename: (code: Code, name: string) => Promise<unknown>
  onToggle: (code: Code, enabled: boolean) => void
  // 만들기. 실패하면 던진다 (겹치는 코드는 R002)
  onAdd: (code: string, name: string) => Promise<unknown>
  disabled: boolean
}) {
  const t = useTranslations("code")
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )
  const codes = group.codes
  const all = codes.map((c) => c.code)
  const allChecked = all.length > 0 && all.every((c) => checked.includes(c))
  const someChecked = !allChecked && all.some((c) => checked.includes(c))
  const nameOf = (id: string | number) => codes.find((c) => c.code === id)?.name ?? String(id)
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
      <Table className="table-fixed">
        <TableHeader>
          <TableRow>
            {/* 좁은 폭에서는 이름 칸이 남도록 다른 칸을 줄인다 */}
            <TableHead className="w-9 sm:w-10">
              <Checkbox
                aria-label={t("selectAll")}
                checked={allChecked}
                indeterminate={someChecked}
                disabled={all.length === 0}
                onCheckedChange={(c) => onCheckedChange(c === true ? all : [])}
              />
            </TableHead>
            <TableHead className="w-9 sm:w-10">
              <span className="sr-only">{t("dragColumn")}</span>
            </TableHead>
            <TableHead className="w-24 sm:w-[34%]">{t("code")}</TableHead>
            <TableHead>{t("name")}</TableHead>
            <TableHead className="w-16 text-right sm:w-20">{t("enabled")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <SortableContext items={all} strategy={verticalListSortingStrategy}>
            {codes.map((code) => (
              <SortableCodeRow
                key={code.code}
                code={code}
                checked={checked.includes(code.code)}
                onCheck={(c) => onCheckedChange(c ? [...checked, code.code] : checked.filter((x) => x !== code.code))}
                onRename={(name) => onRename(code, name)}
                onToggle={(enabled) => onToggle(code, enabled)}
                disabled={disabled}
              />
            ))}
          </SortableContext>
          {codes.length === 0 && (
            <TableRow>
              <TableCell colSpan={5} className="py-6 text-center text-sm text-muted-foreground">
                {t("itemsEmpty")}
              </TableCell>
            </TableRow>
          )}
        </TableBody>
        <TableFooter className="bg-transparent">
          <AddRow key={group.groupCode} onAdd={onAdd} />
        </TableFooter>
      </Table>
    </DndContext>
  )
}

function SortableCodeRow({
  code,
  checked,
  onCheck,
  onRename,
  onToggle,
  disabled,
}: {
  code: Code
  checked: boolean
  onCheck: (checked: boolean) => void
  onRename: (name: string) => Promise<unknown>
  onToggle: (enabled: boolean) => void
  disabled: boolean
}) {
  const t = useTranslations("code")
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: code.code,
    disabled,
  })
  const [editing, setEditing] = useState(false)
  // 엔터와 칸 벗어나기가 함께 오면 한 번만 저장한다
  const [saving, setSaving] = useState(false)

  const commit = async (value: string) => {
    const name = value.trim()
    if (saving) return
    if (name === "" || name === code.name || name.length > 100) return setEditing(false)
    setSaving(true)
    try {
      await onRename(name)
      setEditing(false)
    } finally {
      setSaving(false)
    }
  }

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
        <span
          className={cn("block truncate font-mono text-sm", !code.enabled && "text-muted-foreground")}
          title={code.code}
        >
          {code.code}
        </span>
      </TableCell>
      <TableCell>
        {editing ? (
          <Input
            aria-label={t("nameOf", { code: code.code })}
            defaultValue={code.name}
            maxLength={100}
            autoFocus
            disabled={saving}
            className="h-8"
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault()
                void commit(e.currentTarget.value)
              } else if (e.key === "Escape") {
                e.preventDefault()
                setEditing(false)
              }
            }}
            onBlur={(e) => void commit(e.currentTarget.value)}
          />
        ) : (
          <button
            type="button"
            aria-label={t("editName", { name: code.name })}
            className={cn(
              "group/name flex w-full min-w-0 items-center gap-1.5 rounded-sm px-1 py-1 text-left hover:bg-muted",
              !code.enabled && "text-muted-foreground",
            )}
            onClick={() => setEditing(true)}
          >
            <span className="truncate">{code.name}</span>
            <Pencil
              aria-hidden="true"
              className="size-3.5 shrink-0 text-muted-foreground opacity-0 group-hover/name:opacity-100 group-focus-visible/name:opacity-100"
            />
          </button>
        )}
      </TableCell>
      {/* 스위치의 누름 영역이 칸 밖으로 나가 가로 스크롤이 생기지 않게 오른쪽 여백을 둔다 */}
      <TableCell className="pr-4 text-right">
        <Switch
          aria-label={t("enabledOf", { name: code.name })}
          checked={code.enabled}
          disabled={disabled}
          onCheckedChange={(c) => onToggle(c === true)}
        />
      </TableCell>
    </TableRow>
  )
}

// 맨 아래 추가 줄: 코드(필수)와 이름을 적고 엔터나 "추가"로 맨 뒤에 만든다. 오류는 그 칸 아래에 보인다
function AddRow({ onAdd }: { onAdd: (code: string, name: string) => Promise<unknown> }) {
  const t = useTranslations("code")
  const fieldErrorText = useFieldErrorText()
  const [code, setCode] = useState("")
  const [name, setName] = useState("")
  const [errors, setErrors] = useState<{ code?: string; name?: string }>({})
  const [pending, setPending] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const parsed = codeItemSchema.safeParse({ code, name })
    if (!parsed.success) {
      const issues = parsed.error.issues
      setErrors({
        code: issues.find((i) => i.path[0] === "code")?.message,
        name: issues.find((i) => i.path[0] === "name")?.message,
      })
      return
    }
    setErrors({})
    setPending(true)
    try {
      await onAdd(parsed.data.code, parsed.data.name)
      setCode("")
      setName("")
      document.getElementById("code-add-code")?.focus()
    } catch (error) {
      if (toApiError(error).code === "R002") setErrors({ code: "code.duplicate" })
    } finally {
      setPending(false)
    }
  }
  const errorText = (message: string) => fieldErrorText({ type: "manual", message })

  return (
    <TableRow className="hover:bg-transparent">
      <TableCell colSpan={2} className="align-top text-xs text-muted-foreground">
        <span className="flex h-8 items-center justify-center">
          <Plus aria-hidden="true" className="size-4" />
          <span className="sr-only">{t("addRow")}</span>
        </span>
      </TableCell>
      <TableCell className="align-top">
        <form id="code-add-form" noValidate onSubmit={(e) => void submit(e)} />
        <Input
          id="code-add-code"
          form="code-add-form"
          aria-label={t("code")}
          placeholder={t("codePlaceholder")}
          value={code}
          onChange={(e) => setCode(toCode(e.target.value))}
          maxLength={50}
          autoCapitalize="characters"
          autoComplete="off"
          spellCheck={false}
          className="h-8 font-mono"
          aria-invalid={!!errors.code}
          aria-describedby={errors.code ? "code-add-code-error" : undefined}
        />
        {errors.code && <FieldError id="code-add-code-error" className="mt-1" errors={errorText(errors.code)} />}
      </TableCell>
      <TableCell className="align-top">
        <Input
          form="code-add-form"
          aria-label={t("name")}
          placeholder={t("namePlaceholder")}
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          className="h-8"
          aria-invalid={!!errors.name}
          aria-describedby={errors.name ? "code-add-name-error" : undefined}
        />
        {errors.name && <FieldError id="code-add-name-error" className="mt-1" errors={errorText(errors.name)} />}
      </TableCell>
      <TableCell className="text-right align-top">
        <Button type="submit" form="code-add-form" size="sm" disabled={pending}>
          {t("add")}
        </Button>
      </TableCell>
    </TableRow>
  )
}
