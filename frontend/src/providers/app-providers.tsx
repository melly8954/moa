"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { ThemeProvider } from "next-themes"
import { useState } from "react"
import { ConfirmHost } from "@/components/kit/confirm-dialog"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { STALE_TIME } from "@/lib/query"

// 앱 전체 공급자. 루트 layout.tsx가 감싼다 (문구 공급자 NextIntlClientProvider는 layout.tsx에 있다)
//   ThemeProvider: 화면 모드(밝게·어둡게·기기 설정). <html>에 dark 클래스를 붙인다
//   QueryClient: 서버 데이터 조회·캐시 (useQuery, useMutation)
//   TooltipProvider: Tooltip을 쓰려면 필요하다 (사이드바 접힘 메뉴 이름)
//   Toaster: 잠깐 떴다 사라지는 알림 (toast)
//   ConfirmHost: confirm()이 띄우는 확인 창
export function AppProviders({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { staleTime: STALE_TIME.DEFAULT, retry: 1, refetchOnWindowFocus: false } },
      }),
  )
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          {children}
          <Toaster richColors position="top-center" />
          <ConfirmHost />
        </TooltipProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
