"use client"

import { api } from "@/api"
import { useAuthStore } from "@/stores/auth"

// 로그아웃. 서버 로그아웃이 실패해도(이미 만료 등) 화면에서는 로그아웃한다
// 로그인 화면으로 보내는 일과 캐시 비우기는 AuthGate가 한다 (돌아갈 주소 ?next=는 붙이지 않는다)
export function useSignOut() {
  return async () => {
    await api.v1.auth.logout().catch(() => undefined)
    useAuthStore.getState().signOut(true)
  }
}
