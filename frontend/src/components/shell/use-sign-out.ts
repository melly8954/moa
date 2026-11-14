"use client"

import { api } from "@/api"
import { disablePush } from "@/lib/web-push"
import { useAuthStore } from "@/stores/auth"

// 로그아웃. 그 전에 이 브라우저의 브라우저 알림 구독을 서버에서 지우고 해제한다 (moa 007 R8, 키트로 옮길 것)
// 서버 로그아웃이 실패해도(이미 만료 등) 화면에서는 로그아웃한다
// 로그인 화면으로 보내는 일과 캐시 비우기는 AuthGate가 한다 (돌아갈 주소 ?next=는 붙이지 않는다)
export function useSignOut() {
  return async () => {
    await disablePush().catch(() => undefined)
    await api.v1.auth.logout().catch(() => undefined)
    useAuthStore.getState().signOut(true)
  }
}
