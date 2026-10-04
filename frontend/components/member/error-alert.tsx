import { useTranslations } from "next-intl"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"

/** 서버 오류 코드의 문구(errors.<코드>)를 Alert로 보여 준다. 모르는 코드는 S001 문구 */
export function ErrorAlert({ title, code, children }: { title?: string; code: string; children?: React.ReactNode }) {
  const t = useTranslations("errors")
  const message = t.has(code) ? t(code) : t("S001")
  return (
    <Alert variant="destructive">
      {title ? <AlertTitle>{title}</AlertTitle> : null}
      <AlertDescription>
        <p>{message}</p>
        {children}
      </AlertDescription>
    </Alert>
  )
}
