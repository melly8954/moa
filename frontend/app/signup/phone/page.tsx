import { SignUpPhoneForm } from "@/components/member/sign-up-phone-form"

/** SCR-MEM-005 가입: 생년월일·휴대폰 인증. 이메일 인증 링크(?token=)도 이 화면으로 연다 */
export default async function SignUpPhonePage({ searchParams }: PageProps<"/signup/phone">) {
  const { token } = await searchParams
  return <SignUpPhoneForm token={typeof token === "string" ? token : null} />
}
