"use client"

import { Client, type IMessage } from "@stomp/stompjs"
import { useQueryClient } from "@tanstack/react-query"
import { createContext, useContext, useEffect, useRef, useState } from "react"
import { unsubscribeBrowserOnly } from "@/lib/web-push"
import { useAuthStore } from "@/stores/auth"

// 실시간 연결 (STOMP over WebSocket, specs/007-notifications research R2·R3). moa에서 먼저 만들었다. 키트로 옮길 것
//   로그인 상태면 액세스 토큰을 CONNECT 헤더에 실어 백엔드 /ws에 직접 붙는다 (프론트 프록시는 WebSocket을 넘기지 못한다)
//   로그아웃이면 끊는다. 토큰이 바뀌면 다시 붙는다. 끊기면 5초 뒤 다시 붙고, 다시 이어지면 알림 쿼리를 다시 읽는다
//   로그아웃 상태(signedOut·expired)가 되면 이유와 관계없이 이 브라우저의 웹 푸시 구독도 해제한다 (research R8)
type Realtime = { client: Client | null; connected: boolean }

const RealtimeContext = createContext<Realtime>({ client: null, connected: false })

// 다시 이어질 때 다시 읽을 쿼리 (벨 숫자, 알림 목록, 새 공지 여부)
const RESYNC_KEYS = [["notifications"], ["notices", "new"]] as const

export function RealtimeProvider({ wsUrl, children }: { wsUrl: string; children: React.ReactNode }) {
  const status = useAuthStore((s) => s.status)
  const accessToken = useAuthStore((s) => s.accessToken)
  const queryClient = useQueryClient()
  const [state, setState] = useState<Realtime>({ client: null, connected: false })
  const connectedOnce = useRef(false)

  useEffect(() => {
    if (status === "signedOut" || status === "expired") void unsubscribeBrowserOnly()
  }, [status])

  useEffect(() => {
    if (status !== "signedIn" || !accessToken) return
    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setState({ client, connected: true })
        if (connectedOnce.current) {
          for (const key of RESYNC_KEYS) void queryClient.invalidateQueries({ queryKey: [...key] })
        }
        connectedOnce.current = true
      },
      onWebSocketClose: () => setState({ client, connected: false }),
    })
    client.activate()
    return () => {
      setState({ client: null, connected: false })
      void client.deactivate()
    }
  }, [status, accessToken, wsUrl, queryClient])

  return <RealtimeContext.Provider value={state}>{children}</RealtimeContext.Provider>
}

// 구독 경로를 구독하고 메시지(JSON)를 handler로 넘긴다. 연결이 바뀌면 다시 구독한다
export function useRealtimeSubscription<T>(destination: string, handler: (message: T) => void) {
  const { client, connected } = useContext(RealtimeContext)
  const handlerRef = useRef(handler)
  useEffect(() => {
    handlerRef.current = handler
  })
  useEffect(() => {
    if (!client || !connected) return
    const subscription = client.subscribe(destination, (frame: IMessage) => {
      try {
        handlerRef.current(JSON.parse(frame.body) as T)
      } catch {
        // 모양이 다른 메시지는 버린다
      }
    })
    return () => subscription.unsubscribe()
  }, [client, connected, destination])
}
