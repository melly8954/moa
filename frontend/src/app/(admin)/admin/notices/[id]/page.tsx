"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useParams, useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useMemo } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { confirm } from "@/components/kit/confirm-dialog"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { StatusText } from "@/components/kit/status-text"
import { NoticeForm, type NoticeFormSubmit } from "@/components/notice/notice-form"
import { useNoticeErrorText } from "@/components/notice/use-notice-error"
import { Button } from "@/components/ui/button"
import type { AdminNotice } from "@/types/notice"

// 공지 고치기 (specs/006-notices US1)
//   임시 저장 공지: "저장"·"게시" / 게시한 공지: "저장"만 (게시한 공지는 임시 저장으로 되돌리지 않는다)
//   고정·풀기와 지우기는 바로 반영한다
export default function EditNoticePage() {
  const t = useTranslations("notice")
  const id = Number(useParams<{ id: string }>().id)
  const router = useRouter()
  const queryClient = useQueryClient()
  const errorText = useNoticeErrorText()
  const queryKey = ["admin", "notices", "detail", id]
  const notice = useQuery({ queryKey, queryFn: () => api.v1.noticeAdmin.get(id) })

  const refresh = (updated?: AdminNotice) => {
    if (updated) queryClient.setQueryData(queryKey, updated)
    return queryClient.invalidateQueries({ queryKey: ["admin", "notices"], refetchType: "none" })
  }

  const onSubmit: NoticeFormSubmit = async (values, intent) => {
    try {
      if (intent === "publish") {
        const ok = await confirm({ title: t("publishTitle"), description: t("publishDescription") })
        if (!ok) return false
      }
      let updated = await api.v1.noticeAdmin.modify(id, { title: values.title, content: values.content })
      if (intent === "publish") updated = await api.v1.noticeAdmin.publish(id)
      await refresh(updated)
      toast.success(t(intent === "publish" ? "published" : "saved"))
      return true
    } catch (error) {
      toast.error(errorText(error))
      return false
    }
  }

  const togglePin = useMutation({
    mutationFn: (pinned: boolean) => api.v1.noticeAdmin.pin(id, pinned),
    onSuccess: async (updated) => {
      await refresh(updated)
      toast.success(t(updated.pinned ? "pinnedDone" : "unpinnedDone"))
    },
    onError: (error) => toast.error(errorText(error)),
  })

  const remove = async () => {
    const ok = await confirm({
      title: t("removeTitle"),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (!ok) return
    try {
      await api.v1.noticeAdmin.remove(id)
      await queryClient.invalidateQueries({ queryKey: ["admin", "notices"] })
      toast.success(t("removed"))
      router.replace("/admin/notices")
    } catch (error) {
      toast.error(errorText(error))
    }
  }

  return (
    <>
      <PageHeader
        title={t("editTitle")}
        actions={
          notice.data && (
            <>
              <Button
                variant="outline"
                disabled={togglePin.isPending}
                onClick={() => togglePin.mutate(!notice.data.pinned)}
              >
                {t(notice.data.pinned ? "unpin" : "pin")}
              </Button>
              <Button variant="destructive" onClick={() => void remove()}>
                {t("remove")}
              </Button>
            </>
          )
        }
      />
      <QueryState query={notice}>{(data) => <EditForm notice={data} onSubmit={onSubmit} />}</QueryState>
    </>
  )
}

function EditForm({ notice, onSubmit }: { notice: AdminNotice; onSubmit: NoticeFormSubmit }) {
  const t = useTranslations("notice")
  const published = notice.status === "PUBLISHED"
  // 같은 공지면 폼 값을 다시 넣지 않는다 (고정을 바꿔도 쓰던 글이 지워지지 않게)
  const initial = useMemo(
    () => ({ title: notice.title, content: notice.content, pinned: notice.pinned }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 공지가 바뀔 때만 다시 넣는다
    [notice.id, notice.updatedAt],
  )
  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm">
        <StatusText tone={published ? "positive" : "neutral"}>{t(`statuses.${notice.status}`)}</StatusText>
      </p>
      <NoticeForm
        initial={initial}
        showPinned={false}
        onSubmit={onSubmit}
        actions={
          <>
            <Button type="submit" name="intent" value="save" variant={published ? "default" : "outline"}>
              {t("save")}
            </Button>
            {!published && (
              <Button type="submit" name="intent" value="publish">
                {t("publish")}
              </Button>
            )}
          </>
        }
      />
    </div>
  )
}
