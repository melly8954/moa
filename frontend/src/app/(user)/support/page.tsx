"use client"

import { useQuery } from "@tanstack/react-query"
import { Search } from "lucide-react"
import { useTranslations } from "next-intl"
import { api } from "@/api"
import { LinkButton } from "@/components/kit/link-button"
import { PageHeader } from "@/components/kit/page-header"
import { QueryState } from "@/components/kit/query-state"
import { SegmentedButtons } from "@/components/kit/segmented-buttons"
import { RichTextView } from "@/components/rich-text/rich-text-view"
import { useSupportCategories } from "@/components/support/use-support-categories"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useListSearch } from "@/lib/list-search"
import { useAuthStore } from "@/stores/auth"

// 고객센터 FAQ (specs/017-customer-center US2, FR-013). 로그인 없이 연다
//   분류 단추 줄("전체" + 사용 중인 분류)과 검색어로 거르고, 둘 다 주소에 남는다 (research R11)
//   질문을 누르면 그 자리에서 답이 펼쳐진다. 답을 찾지 못하면 1:1 문의로 잇는다
const DEFAULTS = { category: "", keyword: "" }

export default function SupportPage() {
  const t = useTranslations("support")
  const signedIn = useAuthStore((s) => s.status === "signedIn")
  const search = useListSearch({ defaults: DEFAULTS })
  const categories = useSupportCategories()
  const { category, keyword } = search.filters
  const faqs = useQuery({
    queryKey: ["faqs", category, keyword],
    queryFn: () => api.v1.faq.list({ categoryCode: category || undefined, keyword: keyword.trim() || undefined }),
  })

  const items = [
    { value: "", label: t("categoryAll") },
    ...(categories.data ?? []).map((c) => ({ value: c.code, label: c.name })),
  ]
  const askButton = (
    <LinkButton href="/support/inquiries/new" variant="outline">
      {t("ask")}
    </LinkButton>
  )

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        actions={
          <div className="flex flex-wrap gap-2">
            {signedIn && (
              <LinkButton href="/support/inquiries" variant="ghost">
                {t("myInquiries")}
              </LinkButton>
            )}
            {askButton}
          </div>
        }
      />
      <div className="flex flex-col gap-3">
        <SegmentedButtons
          aria-label={t("category")}
          className="flex-wrap"
          items={items}
          value={category}
          onChange={(value) => search.submit({ category: value })}
        />
        <form
          role="search"
          className="flex max-w-md gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            search.submit()
          }}
        >
          <Input
            type="search"
            aria-label={t("keyword")}
            placeholder={t("keywordPlaceholder")}
            value={search.draft.keyword}
            maxLength={200}
            onChange={(e) => search.setDraft({ keyword: e.target.value })}
          />
          <Button type="submit" variant="outline">
            <Search />
            {t("searchButton")}
          </Button>
        </form>
      </div>
      <QueryState
        query={faqs}
        isEmpty={(data) => data.length === 0}
        empty={
          <div className="flex flex-col items-center gap-3 py-6 text-center">
            <p className="text-sm text-muted-foreground">{t("empty")}</p>
            {askButton}
          </div>
        }
      >
        {(data) => (
          <Accordion className="rounded-md border px-4">
            {data.map((faq) => (
              <AccordionItem key={faq.id} value={faq.id}>
                <AccordionTrigger>
                  <span className="flex min-w-0 flex-col gap-0.5">
                    <span className="text-xs font-normal text-muted-foreground">{faq.categoryName}</span>
                    <span className="break-words">{faq.question}</span>
                  </span>
                </AccordionTrigger>
                <AccordionContent>
                  <RichTextView html={faq.answer} />
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        )}
      </QueryState>
    </>
  )
}
