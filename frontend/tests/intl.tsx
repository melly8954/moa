import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, renderHook } from "@testing-library/react"
import { NextIntlClientProvider } from "next-intl"
import ko from "../messages/ko.json"

// 문구 공급자(ko)와 서버 데이터 공급자(QueryClient)로 감싸 그린다. 화면 컴포넌트 테스트는 render 대신 이것을 쓴다
function wrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return function Providers({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <NextIntlClientProvider locale="ko" messages={ko}>
          {children}
        </NextIntlClientProvider>
      </QueryClientProvider>
    )
  }
}

export function renderWithIntl(ui: React.ReactElement) {
  return render(ui, { wrapper: wrapper() })
}

export function renderHookWithIntl<T>(hook: () => T) {
  return renderHook(hook, { wrapper: wrapper() })
}
