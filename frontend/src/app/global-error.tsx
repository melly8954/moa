"use client"

import { createTranslator } from "next-intl"
import { useEffect } from "react"
import { ErrorView } from "@/components/kit/error-view"
import ko from "../../messages/ko.json"
import "./globals.css"
import "./layout-tokens.css"

// 루트 레이아웃까지 무너졌을 때 보이는 화면. 루트 레이아웃 대신 <html>·<body>를 직접 그린다
// 공급자(문구·화면 모드)가 없으므로 기준 언어(ko) 문구로 보인다
const t = createTranslator({ locale: "ko", messages: ko })

export default function GlobalError({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <html lang="ko" className="h-full antialiased">
      <body className="flex min-h-full flex-col">
        <ErrorView
          title={t("errorPage.title")}
          description={t("errorPage.description")}
          retryLabel={t("common.retry")}
          onRetry={retry}
        />
      </body>
    </html>
  )
}
