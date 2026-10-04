"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { useState } from "react"

/** 서버 상태(TanStack Query)를 앱 전체에 연다 */
export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } }),
  )
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}
