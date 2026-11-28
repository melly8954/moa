"use client"

import { useParams } from "next/navigation"
import { LegalVersionPage } from "@/components/legal/legal-page"

// 이용약관 버전 하나 (specs/012-terms-privacy US3-AC2·AC3). 버전 이름은 주소에서 읽는다
export default function TermsVersionPage() {
  const { version } = useParams<{ version: string }>()
  return <LegalVersionPage document="terms" version={decodeURIComponent(version)} />
}
