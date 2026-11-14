"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { z } from "zod"
import { PageHeader } from "@/components/kit/page-header"
import { useUnsavedChanges } from "@/components/kit/use-unsaved-changes"
import { RichTextEditor } from "@/components/rich-text/rich-text-editor"
import { RichTextView } from "@/components/rich-text/rich-text-view"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"

// 개발용 보기 화면이라 글자를 messages에 두지 않는다 (check:i18n은 (dev)를 보지 않는다)
// 프로젝트 화면은 글자를 모두 messages/ko.json 키로 쓰고, 스키마는 src/lib/schemas/에 둔다

const schema = z.object({ content: z.string().min(1, "본문을 적어 주세요.").max(20000) })
type Input = z.infer<typeof schema>

// 서버에서 받은 글이라 치고, 허용 목록 밖의 것을 섞었다. 보기 부품이 거르는지 본다
const STORED = [
  "<h2>공지 예시</h2>",
  '<p><strong>굵게</strong>, <em>기울임</em>, <s>취소선</s>, <code>코드</code>, <a href="https://example.com" target="_blank">링크</a></p>',
  '<p style="color:red" onclick="alert(1)">색과 클릭 속성은 빠진다 <a href="javascript:alert(1)">위험한 링크</a></p>',
  '<img src="https://tracker.example.com/pixel.png" alt="외부 이미지"><script>alert(1)</script>',
  "<ul><li><p>목록 하나</p></li><li><p>목록 둘</p></li></ul>",
  "<blockquote><p>인용</p></blockquote>",
  "<pre><code>const x = 1</code></pre><hr>",
].join("")

export function RichTextDemo() {
  const [saved, setSaved] = useState(STORED)
  const form = useForm<Input>({ resolver: zodResolver(schema), defaultValues: { content: "" } })
  useUnsavedChanges(form.formState.isDirty)

  const onSubmit = (values: Input) => {
    setSaved(values.content)
    form.reset(values)
  }

  return (
    <>
      <PageHeader
        title="서식 있는 글"
        description="편집기로 쓰고 저장하면 아래 보기에 그린다 (저장은 이 화면 안에서만)"
      />
      <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="flex max-w-3xl min-w-0 flex-col gap-4">
        <Controller
          name="content"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel id="content-label">본문</FieldLabel>
              <RichTextEditor
                {...field}
                id="content"
                placeholder="내용을 적어 주세요"
                aria-labelledby="content-label"
                aria-describedby={fieldState.invalid ? "content-error" : "content-help"}
                aria-invalid={fieldState.invalid}
              />
              {fieldState.invalid ? (
                <FieldError id="content-error" errors={[fieldState.error]} />
              ) : (
                <FieldDescription id="content-help">
                  이미지는 버튼·붙여넣기·끌어오기로 넣는다 (png·jpg·gif·webp, 5MB 이하)
                </FieldDescription>
              )}
            </Field>
          )}
        />
        <div>
          <Button type="submit">저장</Button>
        </div>
      </form>
      <section aria-labelledby="view-title" className="flex max-w-3xl min-w-0 flex-col gap-2">
        <h2 id="view-title" className="text-lg font-semibold">
          보기
        </h2>
        <div className="rounded-md border p-4">
          <RichTextView html={saved} />
        </div>
      </section>
    </>
  )
}
