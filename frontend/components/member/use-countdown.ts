"use client"

import { useEffect, useState } from "react"

/** 목표 시각까지 남은 초. 목표가 없거나 지났으면 0 */
export function useCountdown(target: string | null | undefined): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!target) {
      return
    }
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [target])
  if (!target) {
    return 0
  }
  return Math.max(0, Math.ceil((new Date(target).getTime() - now) / 1000))
}
