"use client"

import { useCallback, useMemo, useSyncExternalStore } from "react"

// 사이드바 묶음 메뉴의 펴짐 상태 { 메뉴key: boolean }를 브라우저에 기억한다 (moa 001 FR-005, research R24. 키트로 옮길 것)
//   서버에는 저장하지 않는다. 메뉴 key(서버 메뉴 id)로 기억해 이름을 바꿔도 유지된다
//   저장소를 못 쓰면(사생활 보호 창 등) 이 탭 안에서만 기억한다
const KEY = "moa:sidebar-open-menus"
const EMPTY = "{}"

export type OpenMenus = Record<string, boolean>

const listeners = new Set<() => void>()
let memory = EMPTY
let storageOk = true

function read(): string {
  if (!storageOk) return memory
  try {
    return window.localStorage.getItem(KEY) ?? memory
  } catch {
    return memory
  }
}

function write(next: OpenMenus) {
  memory = JSON.stringify(next)
  try {
    window.localStorage.setItem(KEY, memory)
  } catch {
    storageOk = false
  }
  listeners.forEach((listener) => listener())
}

function parse(raw: string): OpenMenus {
  try {
    const value: unknown = JSON.parse(raw)
    return value && typeof value === "object" && !Array.isArray(value) ? (value as OpenMenus) : {}
  } catch {
    return {}
  }
}

// 같은 탭은 listeners로, 다른 탭은 storage 이벤트로 알린다
function subscribe(listener: () => void) {
  listeners.add(listener)
  const onStorage = (event: StorageEvent) => {
    if (event.key === KEY || event.key === null) listener()
  }
  window.addEventListener("storage", onStorage)
  return () => {
    listeners.delete(listener)
    window.removeEventListener("storage", onStorage)
  }
}

export function useOpenMenus() {
  // 서버 렌더는 저장값 없이 그리고(지금 화면 묶음만 펴짐), 브라우저에서 바로 맞춘다
  const raw = useSyncExternalStore(subscribe, read, () => EMPTY)
  const openMenus = useMemo(() => parse(raw), [raw])
  const setMenuOpen = useCallback((key: string, open: boolean) => write({ ...parse(read()), [key]: open }), [])
  // 지금 화면이 있어 자동으로 펴진 묶음을 펴진 채로 기억한다. 직접 펴거나 접은 값이 있으면 그대로 둔다 (US1-AC8)
  const rememberOpen = useCallback((key: string) => {
    const current = parse(read())
    if (current[key] === undefined) write({ ...current, [key]: true })
  }, [])
  return { openMenus, setMenuOpen, rememberOpen }
}
