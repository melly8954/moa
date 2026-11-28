"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useParams, useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { confirm } from "@/components/kit/confirm-dialog"
import { LinkButton } from "@/components/kit/link-button"
import { QueryState } from "@/components/kit/query-state"
import { RichTextView } from "@/components/rich-text/rich-text-view"
import { ImageThumbnails } from "@/components/support/image-thumbnails"
import { InquiryStatusText } from "@/components/support/inquiry-status"
import { Button } from "@/components/ui/button"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"

// 문의 상세 (specs/017-customer-center US3, contracts/screens.md "문의 상세"). 로그인 필요
//   답변은 "moa 운영팀"으로 보인다 (답변한 관리자는 보이지 않는다, FR-008). 답변 대기면 지울 수 있다 (FR-009)
//   남의 문의·없는 문의(R001)는 "없는 문의" 안내와 내 문의로 가는 단추 (FR-010, FR-012)
export default function InquiryPage() {
  const t = useTranslations("inquiry")
  const router = useRouter()
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const id = Number(useParams<{ id: string }>().id)
  const inquiry = useQuery({
    queryKey: ["inquiries", "detail", id],
    queryFn: () => api.v1.inquiry.get(id),
    retry: (count, error) => toApiError(error).code !== "R001" && count < 2,
  })
  const missing = inquiry.isError && toApiError(inquiry.error).code === "R001"

  const remove = useMutation({
    mutationFn: () => api.v1.inquiry.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["inquiries", "me"] })
      toast.success(t("removed"))
      router.push("/support/inquiries")
    },
    onError: (error) => toast.error(describeError(toApiError(error)).message),
  })

  const askRemove = async () => {
    const ok = await confirm({
      title: t("removeTitle"),
      description: t("removeDescription"),
      confirmLabel: t("remove"),
      destructive: true,
    })
    if (ok) remove.mutate()
  }

  const backButton = (
    <LinkButton href="/support/inquiries" variant="outline">
      {t("backToList")}
    </LinkButton>
  )

  return (
    <article className="flex min-w-0 flex-col gap-4">
      {missing ? (
        <div className="flex flex-col items-start gap-3">
          <h1 className="text-xl font-semibold">{t("notFound")}</h1>
          {backButton}
        </div>
      ) : (
        <QueryState query={inquiry}>
          {(data) => (
            <>
              <header className="flex flex-col gap-1 border-b pb-3">
                <p className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                  <InquiryStatusText status={data.status} />
                  <span>{data.categoryName}</span>
                </p>
                <h1 className="text-xl font-semibold break-words">{data.title}</h1>
                <p className="text-sm text-muted-foreground">
                  {t("createdAt")} <time dateTime={data.createdAt}>{formatDateTime(data.createdAt)}</time>
                </p>
              </header>
              <p className="text-sm break-words whitespace-pre-wrap">{data.content}</p>
              <ImageThumbnails urls={data.imageUrls} />
              {data.answer ? (
                <section
                  className="flex flex-col gap-2 rounded-md border bg-muted/40 p-4"
                  aria-labelledby="answer-title"
                >
                  <h2 id="answer-title" className="flex flex-wrap items-baseline gap-2 text-sm font-semibold">
                    {t("answerFrom")}
                    {data.answeredAt && (
                      <time dateTime={data.answeredAt} className="font-normal text-muted-foreground">
                        {formatDateTime(data.answeredAt)}
                      </time>
                    )}
                  </h2>
                  <RichTextView html={data.answer} />
                </section>
              ) : (
                <p className="text-sm text-muted-foreground">{t("waitingNote")}</p>
              )}
              <div className="flex flex-wrap gap-2">
                {backButton}
                {data.deletable && (
                  <Button
                    type="button"
                    variant="outline"
                    className="text-destructive"
                    disabled={remove.isPending}
                    onClick={() => void askRemove()}
                  >
                    {t("remove")}
                  </Button>
                )}
              </div>
            </>
          )}
        </QueryState>
      )}
    </article>
  )
}
