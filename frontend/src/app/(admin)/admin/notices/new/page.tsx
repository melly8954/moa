"use client"

import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { api } from "@/api"
import { PageHeader } from "@/components/kit/page-header"
import { NoticeForm, type NoticeFormSubmit } from "@/components/notice/notice-form"
import { useNoticeErrorText } from "@/components/notice/use-notice-error"
import { Button } from "@/components/ui/button"

// 공지 쓰기 (specs/006-notices US1). "임시 저장"은 공지 관리에만, "게시"는 바로 사용자 목록에 보인다
export default function NewNoticePage() {
  const t = useTranslations("notice")
  const router = useRouter()
  const queryClient = useQueryClient()
  const errorText = useNoticeErrorText()

  const onSubmit: NoticeFormSubmit = async (values, intent) => {
    try {
      const notice = await api.v1.noticeAdmin.create({ ...values, publish: intent === "publish" })
      await queryClient.invalidateQueries({ queryKey: ["admin", "notices"] })
      toast.success(t(intent === "publish" ? "published" : "created"))
      router.replace(`/admin/notices/${notice.id}`)
      return true
    } catch (error) {
      toast.error(errorText(error))
      return false
    }
  }

  return (
    <>
      <PageHeader title={t("createTitle")} />
      <NoticeForm
        showPinned
        onSubmit={onSubmit}
        actions={
          <>
            <Button type="submit" name="intent" value="save" variant="outline">
              {t("saveDraft")}
            </Button>
            <Button type="submit" name="intent" value="publish">
              {t("publish")}
            </Button>
          </>
        }
      />
    </>
  )
}
