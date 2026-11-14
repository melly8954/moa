"use client"

import { useTranslations } from "next-intl"
import { PageHeader } from "@/components/kit/page-header"
import { PushToggle } from "@/components/notification/push-toggle"

// 설정 화면 (계정 메뉴 "설정"). 이 브라우저의 브라우저 알림 켜기·끄기 (specs/007-notifications FR-008, US4)
export default function SettingsPage() {
  const t = useTranslations("settings")
  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />
      <PushToggle />
    </>
  )
}
