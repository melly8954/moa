import type { Metadata } from "next"
import { Geist, Geist_Mono } from "next/font/google"
import { NextIntlClientProvider } from "next-intl"
import { getLocale } from "next-intl/server"
import { SITE } from "@/lib/site"
import { AppProviders } from "@/providers/app-providers"
import "./globals.css"
import "./layout-tokens.css"

const sans = Geist({ variable: "--font-sans", subsets: ["latin"] })
const mono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] })

export const metadata: Metadata = {
  title: { default: SITE.name, template: `%s | ${SITE.name}` },
}

// 셸은 여기에 두지 않는다. 로그인 화면은 (public), 로그인 뒤 화면은 (main) 레이아웃이 감싼다
// 언어는 쿠키에서 읽는다 (src/i18n/request.ts). suppressHydrationWarning: 화면 모드가 <html> 클래스를 바꾼다
export default async function RootLayout({ children }: LayoutProps<"/">) {
  const locale = await getLocale()
  return (
    <html lang={locale} className={`${sans.variable} ${mono.variable} h-full antialiased`} suppressHydrationWarning>
      <body className="flex min-h-full flex-col">
        <NextIntlClientProvider>
          <AppProviders>{children}</AppProviders>
        </NextIntlClientProvider>
      </body>
    </html>
  )
}
