import { getTranslations } from "next-intl/server"
import { LocaleSwitcher } from "@/components/member/locale-switcher"
import { SignUpFlowProvider } from "@/components/member/sign-up-flow"

/** 가입 단계 화면: 셸 없이 가운데 카드 (docs/design/ui/ui-rules.md 예외) */
export default async function SignUpLayout({ children }: LayoutProps<"/signup">) {
  const t = await getTranslations("common")
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-4 py-8">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <div className="flex flex-col items-center gap-1 text-center">
          <p className="text-2xl font-semibold">{t("brand")}</p>
          <p className="text-sm text-muted-foreground">{t("tagline")}</p>
        </div>
        <SignUpFlowProvider>{children}</SignUpFlowProvider>
        <div className="flex justify-center">
          <LocaleSwitcher />
        </div>
      </div>
    </main>
  )
}
