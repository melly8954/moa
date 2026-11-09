import { useTranslations } from "next-intl"
import { LinkButton } from "@/components/kit/link-button"

export default function NotFound() {
  const t = useTranslations()
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 p-4 text-center">
      <h1 className="text-xl font-semibold">{t("notFound.title")}</h1>
      <p className="text-sm text-muted-foreground">{t("notFound.description")}</p>
      <LinkButton href="/">{t("common.home")}</LinkButton>
    </main>
  )
}
