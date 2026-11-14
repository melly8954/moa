"use client"

import Image from "@tiptap/extension-image"
import { Placeholder } from "@tiptap/extensions"
import { type Editor, EditorContent, useEditor, useEditorState } from "@tiptap/react"
import StarterKit from "@tiptap/starter-kit"
import {
  Bold,
  Code,
  Heading2,
  Heading3,
  ImagePlus,
  Italic,
  Link,
  List,
  ListOrdered,
  Minus,
  Quote,
  Redo2,
  SquareCode,
  Strikethrough,
  Undo2,
  Unlink,
} from "lucide-react"
import { useTranslations } from "next-intl"
import { useEffect, useId, useImperativeHandle, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { Toggle } from "@/components/ui/toggle"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { isAllowedLink, RICH_TEXT_IMAGE_TYPES, sanitizeRichText } from "@/lib/rich-text"
import { cn } from "@/lib/utils"
import { RICH_TEXT_CONTENT_CLASS } from "./content-class"
import { LinkDialog } from "./link-dialog"
import { useImageUpload } from "./use-image-upload"

// 서식 있는 글 편집기 (Tiptap). react-hook-form Controller에 그대로 잇는다
//   <Controller name="content" control={form.control} render={({ field, fieldState }) => (
//     <Field data-invalid={fieldState.invalid}>
//       <FieldLabel id="content-label">{t("content")}</FieldLabel>
//       <RichTextEditor {...field} aria-labelledby="content-label" aria-invalid={fieldState.invalid} />
//     </Field>
//   )} />
// 값은 HTML 문자열이다. 비어 있으면 "" (Zod .min(1)로 필수를 막는다)
// 도구는 백엔드 허용 목록(lib/rich-text.ts)에 있는 서식만 둔다. 색·글꼴·표·정렬은 서버가 지우므로 두지 않는다
// 이미지: 도구 막대 버튼·붙여넣기·끌어오기 → POST /api/v1/rich-text/images → 받은 주소를 img src로 넣는다
// 저장하지 않고 떠날 때 되묻기는 폼의 isDirty로 한다 (useUnsavedChanges(form.formState.isDirty))

export type RichTextEditorHandle = { focus: () => void }

type RichTextEditorProps = {
  value: string
  onChange: (html: string) => void
  onBlur?: () => void
  /** react-hook-form이 오류 칸으로 옮길 때 focus()를 부른다 */
  ref?: React.Ref<RichTextEditorHandle>
  id?: string
  disabled?: boolean
  placeholder?: string
  className?: string
  "aria-label"?: string
  "aria-labelledby"?: string
  "aria-describedby"?: string
  "aria-invalid"?: boolean
}

const imageFiles = (files: FileList | null | undefined) =>
  Array.from(files ?? []).filter((file) => file.type.startsWith("image/"))

export function RichTextEditor({
  value,
  onChange,
  onBlur,
  ref,
  id,
  disabled = false,
  placeholder,
  className,
  "aria-label": ariaLabel,
  "aria-labelledby": ariaLabelledBy,
  "aria-describedby": ariaDescribedBy,
  "aria-invalid": ariaInvalid,
}: RichTextEditorProps) {
  const t = useTranslations("richText")
  const upload = useImageUpload()
  const [uploading, setUploading] = useState(0)
  const generatedId = useId()
  const contentId = id ?? `rich-text-${generatedId}`

  // 파일을 차례로 올려 pos(없으면 지금 커서)에 넣는다. 올리지 못한 파일은 알림만 띄우고 건너뛴다
  const insertImages = async (target: Editor, files: File[], pos?: number) => {
    for (const file of files) {
      setUploading((n) => n + 1)
      const url = await upload(file).finally(() => setUploading((n) => n - 1))
      if (!url || target.isDestroyed) continue
      const image = { type: "image", attrs: { src: url, alt: "" } }
      if (pos === undefined) target.chain().focus().insertContent(image).run()
      else target.chain().focus().insertContentAt(pos, image).run()
    }
  }
  // 편집기 설정은 처음 한 번 만들어지므로, 붙여넣기·끌어오기는 늘 최신 함수를 부르게 ref로 잇는다
  const insertImagesRef = useRef(insertImages)
  const editorRef = useRef<Editor | null>(null)

  const editor = useEditor({
    // 서버 렌더와 브라우저 첫 렌더가 같도록 브라우저에서 만든다 (Next)
    immediatelyRender: false,
    editable: !disabled,
    extensions: [
      StarterKit.configure({
        heading: { levels: [2, 3] },
        underline: false,
        link: {
          openOnClick: false,
          autolink: true,
          defaultProtocol: "https",
          isAllowedUri: (url) => isAllowedLink(url),
          HTMLAttributes: { target: null, rel: "noopener noreferrer" },
        },
      }),
      Image.configure({ inline: false, allowBase64: false }),
      Placeholder.configure({ placeholder: placeholder ?? "" }),
    ],
    content: value,
    editorProps: {
      attributes: {
        id: contentId,
        role: "textbox",
        "aria-multiline": "true",
        ...(ariaLabel && { "aria-label": ariaLabel }),
        ...(ariaLabelledBy && { "aria-labelledby": ariaLabelledBy }),
        ...(ariaDescribedBy && { "aria-describedby": ariaDescribedBy }),
        ...(ariaInvalid && { "aria-invalid": "true" }),
        class: cn(
          RICH_TEXT_CONTENT_CLASS,
          "min-h-40 px-3 py-2 outline-none",
          "[&_p.is-editor-empty:first-child]:before:pointer-events-none [&_p.is-editor-empty:first-child]:before:float-left [&_p.is-editor-empty:first-child]:before:h-0 [&_p.is-editor-empty:first-child]:before:text-muted-foreground [&_p.is-editor-empty:first-child]:before:content-[attr(data-placeholder)]",
          "[&_img.ProseMirror-selectednode]:ring-2 [&_img.ProseMirror-selectednode]:ring-ring",
        ),
      },
      // 다른 곳에서 복사한 HTML도 허용 목록으로 거른다 (외부 이미지, 색, 표 등은 빠진다)
      transformPastedHTML: (html) => sanitizeRichText(html),
      handlePaste: (_view, event) => {
        const files = imageFiles(event.clipboardData?.files)
        if (files.length === 0 || !editorRef.current) return false
        void insertImagesRef.current(editorRef.current, files)
        return true
      },
      handleDrop: (view, event, _slice, moved) => {
        const files = moved ? [] : imageFiles(event.dataTransfer?.files)
        if (files.length === 0 || !editorRef.current) return false
        event.preventDefault()
        const pos = view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos
        void insertImagesRef.current(editorRef.current, files, pos)
        return true
      },
    },
    onUpdate: ({ editor: current }) => onChange(current.isEmpty ? "" : current.getHTML()),
    onBlur: () => onBlur?.(),
  })

  useEffect(() => {
    insertImagesRef.current = insertImages
    editorRef.current = editor
  })

  useImperativeHandle(ref, () => ({ focus: () => editor?.commands.focus() }), [editor])

  // 밖에서 값이 바뀌면(form.reset으로 불러온 글 등) 편집기에 넣는다. 사용자가 고친 값이 돌아온 것이면 그대로 둔다
  useEffect(() => {
    if (!editor || editor.isDestroyed) return
    const current = editor.isEmpty ? "" : editor.getHTML()
    if (value !== current) editor.commands.setContent(value || "", { emitUpdate: false })
  }, [editor, value])

  useEffect(() => {
    if (editor && !editor.isDestroyed && editor.isEditable === disabled) editor.setEditable(!disabled, false)
  }, [editor, disabled])

  return (
    <div
      data-slot="rich-text-editor"
      data-disabled={disabled || undefined}
      className={cn(
        "flex min-w-0 flex-col rounded-md border border-input transition-[color,box-shadow] focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 data-disabled:opacity-50",
        ariaInvalid && "border-destructive focus-within:ring-destructive/20",
        className,
      )}
    >
      {editor ? (
        <>
          <Toolbar
            editor={editor}
            contentId={contentId}
            disabled={disabled}
            uploading={uploading > 0}
            onImages={(files) => void insertImages(editor, files)}
          />
          <EditorContent editor={editor} />
          {uploading > 0 && (
            <p role="status" className="flex items-center gap-2 border-t px-3 py-1.5 text-xs text-muted-foreground">
              <Spinner className="size-3" />
              {t("uploading")}
            </p>
          )}
        </>
      ) : (
        // 편집기를 만드는 동안 같은 높이를 잡아 둔다
        <div className="min-h-50" aria-hidden />
      )}
    </div>
  )
}

function Toolbar({
  editor,
  contentId,
  disabled,
  uploading,
  onImages,
}: {
  editor: Editor
  contentId: string
  disabled: boolean
  uploading: boolean
  onImages: (files: File[]) => void
}) {
  const t = useTranslations("richText")
  const fileInput = useRef<HTMLInputElement>(null)
  const [linkOpen, setLinkOpen] = useState(false)
  const state = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      heading2: e.isActive("heading", { level: 2 }),
      heading3: e.isActive("heading", { level: 3 }),
      bold: e.isActive("bold"),
      italic: e.isActive("italic"),
      strike: e.isActive("strike"),
      code: e.isActive("code"),
      bulletList: e.isActive("bulletList"),
      orderedList: e.isActive("orderedList"),
      blockquote: e.isActive("blockquote"),
      codeBlock: e.isActive("codeBlock"),
      link: e.isActive("link"),
      href: (e.getAttributes("link").href as string | undefined) ?? "",
      canUndo: e.can().undo(),
      canRedo: e.can().redo(),
    }),
  })
  const chain = () => editor.chain().focus()

  const applyLink = (href: string) => {
    setLinkOpen(false)
    if (editor.state.selection.empty && !editor.isActive("link")) {
      // 고른 글자가 없으면 주소를 글자로 넣는다
      chain()
        .insertContent({ type: "text", text: href, marks: [{ type: "link", attrs: { href } }] })
        .run()
    } else {
      chain().extendMarkRange("link").setLink({ href }).run()
    }
  }
  const removeLink = () => {
    setLinkOpen(false)
    chain().extendMarkRange("link").unsetLink().run()
  }

  return (
    <div
      role="toolbar"
      aria-label={t("toolbar")}
      aria-controls={contentId}
      className="flex flex-wrap items-center gap-0.5 border-b p-1"
    >
      <Mark
        label={t("heading2")}
        pressed={state.heading2}
        disabled={disabled}
        onPress={() => chain().toggleHeading({ level: 2 }).run()}
      >
        <Heading2 />
      </Mark>
      <Mark
        label={t("heading3")}
        pressed={state.heading3}
        disabled={disabled}
        onPress={() => chain().toggleHeading({ level: 3 }).run()}
      >
        <Heading3 />
      </Mark>
      <Mark label={t("bold")} pressed={state.bold} disabled={disabled} onPress={() => chain().toggleBold().run()}>
        <Bold />
      </Mark>
      <Mark label={t("italic")} pressed={state.italic} disabled={disabled} onPress={() => chain().toggleItalic().run()}>
        <Italic />
      </Mark>
      <Mark label={t("strike")} pressed={state.strike} disabled={disabled} onPress={() => chain().toggleStrike().run()}>
        <Strikethrough />
      </Mark>
      <Mark label={t("code")} pressed={state.code} disabled={disabled} onPress={() => chain().toggleCode().run()}>
        <Code />
      </Mark>
      <Mark
        label={t("bulletList")}
        pressed={state.bulletList}
        disabled={disabled}
        onPress={() => chain().toggleBulletList().run()}
      >
        <List />
      </Mark>
      <Mark
        label={t("orderedList")}
        pressed={state.orderedList}
        disabled={disabled}
        onPress={() => chain().toggleOrderedList().run()}
      >
        <ListOrdered />
      </Mark>
      <Mark
        label={t("blockquote")}
        pressed={state.blockquote}
        disabled={disabled}
        onPress={() => chain().toggleBlockquote().run()}
      >
        <Quote />
      </Mark>
      <Mark
        label={t("codeBlock")}
        pressed={state.codeBlock}
        disabled={disabled}
        onPress={() => chain().toggleCodeBlock().run()}
      >
        <SquareCode />
      </Mark>
      <Action label={t("horizontalRule")} disabled={disabled} onClick={() => chain().setHorizontalRule().run()}>
        <Minus />
      </Action>
      <Mark label={t("link")} pressed={state.link} disabled={disabled} onPress={() => setLinkOpen(true)}>
        <Link />
      </Mark>
      <Action label={t("unlink")} disabled={disabled || !state.link} onClick={removeLink}>
        <Unlink />
      </Action>
      <Action label={t("image")} disabled={disabled || uploading} onClick={() => fileInput.current?.click()}>
        <ImagePlus />
      </Action>
      <Action label={t("undo")} disabled={disabled || !state.canUndo} onClick={() => chain().undo().run()}>
        <Undo2 />
      </Action>
      <Action label={t("redo")} disabled={disabled || !state.canRedo} onClick={() => chain().redo().run()}>
        <Redo2 />
      </Action>
      <input
        ref={fileInput}
        type="file"
        accept={RICH_TEXT_IMAGE_TYPES.join(",")}
        multiple
        hidden
        data-slot="rich-text-image-input"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? [])
          event.target.value = ""
          if (files.length > 0) onImages(files)
        }}
      />
      <LinkDialog
        open={linkOpen}
        onOpenChange={setLinkOpen}
        initialHref={state.href}
        onApply={applyLink}
        onRemove={state.link ? removeLink : undefined}
      />
    </div>
  )
}

// 켜고 끄는 서식 (aria-pressed)
function Mark({
  label,
  pressed,
  disabled,
  onPress,
  children,
}: {
  label: string
  pressed: boolean
  disabled: boolean
  onPress: () => void
  children: React.ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <Toggle
            type="button"
            size="sm"
            aria-label={label}
            pressed={pressed}
            disabled={disabled}
            onPressedChange={onPress}
          />
        }
      >
        {children}
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  )
}

// 한 번 하는 동작 (구분선, 링크 빼기, 이미지, 되돌리기)
function Action({
  label,
  disabled,
  onClick,
  children,
}: {
  label: string
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label={label}
            disabled={disabled}
            onClick={onClick}
          />
        }
      >
        {children}
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  )
}
