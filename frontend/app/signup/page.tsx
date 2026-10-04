import Link from "next/link"
import { getTranslations } from "next-intl/server"
import { ErrorAlert } from "@/components/member/error-alert"
import { SignUpStep } from "@/components/member/sign-up-step"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { oauthSignUpUrl } from "@/lib/api/oauth"

/** SCR-MEM-002 가입 수단 선택. 구글·카카오 인증이 실패하면 서버가 ?error=<코드>를 붙여 돌려보낸다 */
export default async function SignUpMethodPage({ searchParams }: PageProps<"/signup">) {
  const t = await getTranslations("signUp.method")
  const { error } = await searchParams
  const errorCode = typeof error === "string" ? error : null
  const errorTitle = errorCode === "B003" ? t("conflictTitle") : t("oauthFailedTitle")

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h1>{t("title")}</h1>
        </CardTitle>
        <CardDescription>{t("description")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <SignUpStep step="method" />
        {errorCode ? <ErrorAlert title={errorTitle} code={errorCode} /> : null}
        <div className="flex flex-col gap-2">
          <Link href="/signup/email" className={buttonVariants({ size: "lg" })}>
            {t("email")}
          </Link>
          <a href={oauthSignUpUrl("google")} className={buttonVariants({ variant: "outline", size: "lg" })}>
            {t("google")}
          </a>
          <a href={oauthSignUpUrl("kakao")} className={buttonVariants({ variant: "outline", size: "lg" })}>
            {t("kakao")}
          </a>
        </div>
      </CardContent>
      <CardFooter className="justify-center gap-1 text-sm text-muted-foreground">
        {t("haveAccount")}
        <Link href="/login" className={buttonVariants({ variant: "link", size: "sm" })}>
          {t("login")}
        </Link>
      </CardFooter>
    </Card>
  )
}
