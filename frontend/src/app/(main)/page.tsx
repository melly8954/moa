import { useTranslations } from "next-intl"
import { PageHeader } from "@/components/kit/page-header"
import { NewNoticeAlert } from "@/components/notice/new-notice-alert"
import { Card, CardContent } from "@/components/ui/card"

// 로그인 뒤 첫 화면. 프로젝트의 첫 화면으로 바꾼다
export default function HomePage() {
  const t = useTranslations("home")
  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />
      <NewNoticeAlert />
      <Card>
        <CardContent className="text-sm text-muted-foreground">{t("body")}</CardContent>
      </Card>
    </>
  )
}
