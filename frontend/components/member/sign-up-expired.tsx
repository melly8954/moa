import Link from "next/link"
import { useTranslations } from "next-intl"
import { ErrorAlert } from "@/components/member/error-alert"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

/** 가입 진행이 없거나 만료됐을 때(A006). 처음부터 다시 가입하게 한다 */
export function SignUpExpired() {
  const t = useTranslations("signUp.expired")
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <ErrorAlert code="A006" />
        <Link href="/signup" className={buttonVariants({ size: "lg" })}>
          {t("restart")}
        </Link>
      </CardContent>
    </Card>
  )
}
