"use client"

import { useSyncExternalStore } from "react"

function subscribe() {
  return () => {}
}

/** 브라우저에서 화면이 살아난(hydration) 뒤에 true. 그 전에는 입력을 막아 입력값이나 누름이 사라지지 않게 한다 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  )
}
