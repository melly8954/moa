import type { AdminMenu, MenuOrder } from "@/types/menu"

// 메뉴 트리의 순서·부모를 다루는 순수 함수 (moa, specs/001-admin-menus FR-010, research R19·R20)
// 화면은 바뀐 구조를 PUT /menus/order 한 번으로 보낸다 (서버가 2단계·대상 규칙을 다시 본다)

// 1단계 메뉴와 그 하위 메뉴 ID (순서대로)
export type MenuStructure = { id: number; children: number[] }[]

export type MenuRow = { menu: AdminMenu; depth: 0 | 1 }

// 연결 화면이 없으면 묶음 메뉴다
export const isGroup = (menu: Pick<AdminMenu, "screenKey">) => !menu.screenKey

export function structureOf(tree: AdminMenu[]): MenuStructure {
  return tree.map((top) => ({ id: top.id, children: top.children.map((child) => child.id) }))
}

// 표에 그릴 줄 (묶음 바로 아래에 하위 메뉴)
export function rowsOf(tree: AdminMenu[]): MenuRow[] {
  return tree.flatMap((top) => [
    { menu: top, depth: 0 as const },
    ...top.children.map((child) => ({ menu: child, depth: 1 as const })),
  ])
}

// 구조 → 순서 요청. 같은 단계 안에서 10, 20, 30 …
export function ordersOf(structure: MenuStructure): MenuOrder[] {
  return structure.flatMap((top, i) => [
    { id: top.id, parentId: null, sortOrder: (i + 1) * 10 },
    ...top.children.map((id, j) => ({ id, parentId: top.id, sortOrder: (j + 1) * 10 })),
  ])
}

function arrayMove<T>(list: T[], from: number, to: number): T[] {
  const next = [...list]
  const [moved] = next.splice(from, 1)
  next.splice(to, 0, moved)
  return next
}

// 끌어 놓은 뒤의 구조. 바뀌지 않으면 null
//   묶음 메뉴는 1단계 안에서만 하위 메뉴와 함께 옮긴다
//   화면 메뉴는 놓은 자리 바로 위가 묶음이거나 묶음의 하위면 그 묶음 안으로, 아니면 1단계로 간다
export function moveOnDrop(
  structure: MenuStructure,
  groupIds: Set<number>,
  activeId: number,
  overId: number,
): MenuStructure | null {
  if (activeId === overId) return null
  const parentOf = new Map<number, number | null>()
  for (const top of structure) {
    parentOf.set(top.id, null)
    for (const child of top.children) parentOf.set(child, top.id)
  }
  if (!parentOf.has(activeId) || !parentOf.has(overId)) return null

  if (groupIds.has(activeId)) {
    const tops = structure.map((top) => top.id)
    const overTop = parentOf.get(overId) ?? overId
    const from = tops.indexOf(activeId)
    const to = tops.indexOf(overTop)
    if (from === to) return null
    const byId = new Map(structure.map((top) => [top.id, top]))
    return arrayMove(tops, from, to).map((id) => byId.get(id)!)
  }

  const flat = structure.flatMap((top) => [top.id, ...top.children])
  const from = flat.indexOf(activeId)
  const to = flat.indexOf(overId)
  const moved = arrayMove(flat, from, to)
  const prev = moved[to - 1]
  const newParent = prev === undefined ? null : groupIds.has(prev) ? prev : (parentOf.get(prev) ?? null)

  const next: MenuStructure = []
  const byTop = new Map<number, { id: number; children: number[] }>()
  for (const id of moved) {
    const parent = id === activeId ? newParent : (parentOf.get(id) ?? null)
    if (parent === null) {
      const top = { id, children: [] as number[] }
      next.push(top)
      byTop.set(id, top)
    }
  }
  for (const id of moved) {
    const parent = id === activeId ? newParent : (parentOf.get(id) ?? null)
    if (parent !== null) byTop.get(parent)?.children.push(id)
  }
  return next
}

// 고른 화면 메뉴들을 묶음(targetId) 맨 뒤로, targetId가 null이면 1단계 맨 뒤로 옮긴 구조
export function moveToGroup(structure: MenuStructure, ids: number[], targetId: number | null): MenuStructure {
  const moving = new Set(ids)
  const next: MenuStructure = structure
    .filter((top) => !moving.has(top.id))
    .map((top) => ({ id: top.id, children: top.children.filter((id) => !moving.has(id)) }))
  const ordered = structure.flatMap((top) => [top.id, ...top.children]).filter((id) => moving.has(id))
  if (targetId === null) next.push(...ordered.map((id) => ({ id, children: [] })))
  else next.find((top) => top.id === targetId)?.children.push(...ordered)
  return next
}
