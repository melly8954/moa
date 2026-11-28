"use client"

import { useQueryClient } from "@tanstack/react-query"
import { WifiOff } from "lucide-react"
import { useTranslations } from "next-intl"
import { useEffect, useRef, useState } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { QueryState } from "@/components/kit/query-state"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import { Button } from "@/components/ui/button"
import { Message, MessageContent, MessageFooter, MessageHeader } from "@/components/ui/message"
import {
  MessageScroller,
  MessageScrollerButton,
  MessageScrollerContent,
  MessageScrollerItem,
  MessageScrollerProvider,
  MessageScrollerViewport,
  useMessageScrollerScrollable,
} from "@/components/ui/message-scroller"
import { Textarea } from "@/components/ui/textarea"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import { useMe } from "@/lib/me"
import { useRealtimeConnected } from "@/providers/realtime-provider"
import type { PartyMessage } from "@/types/party"
import { appendMessages, type ChatCache, loadOlderMessages, usePartyMessages } from "./use-party"

export const MESSAGE_MAX = 500

// 파티 채팅 (T024, spec.md 화면 "파티 화면 - 채팅"). 멤버에게만 보인다
//   오래된 것이 위, 새것이 아래. 위 끝에 닿으면 before로 더 불러온다 (버튼으로도 부른다)
//   내 메시지는 오른쪽, 안내(SYSTEM)는 가운데 작은 글씨로 party.chat.system 문구를 쓴다
//   남은 글자 수를 보이고 500자를 넘거나 비면 보내지 못한다
//   연결이 끊기면 "끊김"을 보인다. 다시 이으면 채팅 쿼리가 after로 빠진 것을 채운다 (use-party.ts)
export function ChatPanel({ partyId }: { partyId: number }) {
  const t = useTranslations("party.chat")
  const messages = usePartyMessages(partyId, true)
  const connected = useRealtimeConnected()

  return (
    <section aria-labelledby="party-chat" className="flex min-h-0 flex-col gap-2 rounded-lg border p-3">
      <div className="flex items-center justify-between gap-2">
        <h2 id="party-chat" className="font-medium">
          {t("title")}
        </h2>
        {!connected && (
          <span role="status" className="flex items-center gap-1 text-xs text-destructive">
            <WifiOff className="size-4" aria-hidden />
            {t("disconnected")}
          </span>
        )}
      </div>
      <QueryState query={messages}>
        {(data) => (
          <MessageScrollerProvider autoScroll defaultScrollPosition="end">
            <ChatMessages partyId={partyId} data={data} />
          </MessageScrollerProvider>
        )}
      </QueryState>
      <ChatInput partyId={partyId} />
    </section>
  )
}

function ChatMessages({ partyId, data }: { partyId: number; data: ChatCache }) {
  const t = useTranslations("party.chat")
  const queryClient = useQueryClient()
  const myId = useMe().data?.id
  const scrollable = useMessageScrollerScrollable()
  const [loading, setLoading] = useState(false)

  const loadOlder = async () => {
    if (loading || !data.hasOlder) return
    setLoading(true)
    try {
      await loadOlderMessages(queryClient, partyId)
    } catch {
      toast.error(t("loadOlderFailed"))
    } finally {
      setLoading(false)
    }
  }

  // 위 끝에 닿으면(더 올릴 곳이 없으면) 이전 대화를 부른다
  const atTop = !scrollable.start
  const loadRef = useRef(loadOlder)
  useEffect(() => {
    loadRef.current = loadOlder
  })
  useEffect(() => {
    if (atTop && data.hasOlder) void loadRef.current()
  }, [atTop, data.hasOlder])

  return (
    <MessageScroller className="h-96 max-h-[60vh]">
      <MessageScrollerViewport preserveScrollOnPrepend aria-label={t("messageList")}>
        <MessageScrollerContent className="gap-3 p-1">
          {data.hasOlder && (
            <div className="flex justify-center">
              <Button variant="ghost" size="sm" disabled={loading} onClick={() => void loadOlder()}>
                {t("loadOlder")}
              </Button>
            </div>
          )}
          {data.items.length === 0 && <p className="text-center text-sm text-muted-foreground">{t("empty")}</p>}
          {data.items.map((m) => (
            <MessageScrollerItem key={m.id} messageId={String(m.id)}>
              <ChatMessage message={m} mine={m.type === "CHAT" && m.sender?.userId === myId} />
            </MessageScrollerItem>
          ))}
        </MessageScrollerContent>
      </MessageScrollerViewport>
      <MessageScrollerButton direction="end" />
    </MessageScroller>
  )
}

function ChatMessage({ message, mine }: { message: PartyMessage; mine: boolean }) {
  const t = useTranslations("party.chat")
  const time = <time dateTime={message.sentAt}>{formatDateTime(message.sentAt)}</time>

  if (message.type === "SYSTEM") {
    const name = message.subject?.displayName ?? ""
    return (
      <p className="text-center text-xs text-muted-foreground">
        {message.systemEvent ? t(`system.${message.systemEvent}`, { name }) : message.content}{" "}
        <span className="sr-only">{time}</span>
      </p>
    )
  }

  return (
    <Message align={mine ? "end" : "start"}>
      <MessageContent>
        <MessageHeader>{mine ? t("me") : message.sender?.displayName}</MessageHeader>
        <Bubble variant={mine ? "default" : "muted"} align={mine ? "end" : "start"}>
          <BubbleContent className="whitespace-pre-wrap">{message.content}</BubbleContent>
        </Bubble>
        <MessageFooter>{time}</MessageFooter>
      </MessageContent>
    </Message>
  )
}

function ChatInput({ partyId }: { partyId: number }) {
  const t = useTranslations("party.chat")
  const queryClient = useQueryClient()
  const { describeError } = useErrorMessages()
  const [content, setContent] = useState("")
  const [error, setError] = useState<string>()
  const [sending, setSending] = useState(false)
  const length = content.length
  const tooLong = length > MESSAGE_MAX
  const blank = content.trim() === ""

  const send = async () => {
    if (tooLong || blank || sending) return
    setSending(true)
    setError(undefined)
    try {
      const message = await api.v1.party.sendMessage(partyId, content)
      appendMessages(queryClient, partyId, [message])
      setContent("")
    } catch (e) {
      const apiError = toApiError(e)
      setError(apiError.fieldErrors[0]?.message ? t("invalid") : describeError(apiError).message)
    } finally {
      setSending(false)
    }
  }

  return (
    <form
      className="flex flex-col gap-1"
      onSubmit={(e) => {
        e.preventDefault()
        void send()
      }}
    >
      {error && (
        <Alert variant="destructive" role="alert">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <div className="flex items-end gap-2">
        <Textarea
          aria-label={t("inputLabel")}
          aria-describedby="party-chat-count"
          aria-invalid={tooLong}
          value={content}
          rows={2}
          placeholder={t("placeholder")}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault()
              void send()
            }
          }}
        />
        <Button type="submit" disabled={tooLong || blank || sending}>
          {t("send")}
        </Button>
      </div>
      <p
        id="party-chat-count"
        className={tooLong ? "text-xs text-destructive" : "text-xs text-muted-foreground"}
        aria-live="polite"
      >
        {tooLong ? t("tooLong", { max: MESSAGE_MAX }) : t("remaining", { count: MESSAGE_MAX - length })}
      </p>
    </form>
  )
}
