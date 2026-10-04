import { useTranslations } from "next-intl"

const STEPS = ["method", "email", "phone", "profile"] as const
export type SignUpStepName = (typeof STEPS)[number]

/** 가입 단계 표시 (예: 회원가입 2/4 이메일 인증) */
export function SignUpStep({ step }: { step: SignUpStepName }) {
  const t = useTranslations("signUp")
  const current = STEPS.indexOf(step) + 1
  return (
    <p className="text-xs text-muted-foreground">
      {t("progress", { current, total: STEPS.length })} {t(`steps.${step}`)}
    </p>
  )
}
