"use client"

import { useParams } from "next/navigation"
import { LegalVersionPage } from "@/components/legal/legal-page"

// 개인정보처리방침 버전 하나 (specs/012-terms-privacy US3-AC2·AC3). 버전 이름은 주소에서 읽는다
export default function PrivacyVersionPage() {
  const { version } = useParams<{ version: string }>()
  return <LegalVersionPage document="privacy" version={decodeURIComponent(version)} />
}
