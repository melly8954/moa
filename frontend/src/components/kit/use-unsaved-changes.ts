"use client"

import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { useCallback, useEffect } from "react"
import { confirm } from "./confirm-dialog"

// 저장하지 않은 입력이 있을 때 화면을 떠나기 전에 되묻는다 (등록·수정 폼은 모두 쓴다)
//   const { confirmLeave } = useUnsavedChanges(form.formState.isDirty)
// 막는 것
//   - 사이트 안 링크 누름(메뉴 등): 확인 창을 띄우고, 나가기를 고르면 그 주소로 간다
//   - 새로고침·탭 닫기·다른 사이트로 가기: 브라우저 기본 경고
//   - 코드로 옮기는 곳(취소 버튼 등): if (await confirmLeave()) router.push("/orders")
// 막지 못하는 것: 브라우저 뒤로 가기 버튼 (Next App Router가 막을 길을 주지 않는다)
// 저장에 성공하면 form.reset()으로 dirty를 풀고 옮긴다
export function useUnsavedChanges(dirty: boolean) {
  const t = useTranslations("unsaved")
  const router = useRouter()

  const confirmLeave = useCallback(
    async () =>
      !dirty ||
      confirm({ title: t("title"), description: t("description"), confirmLabel: t("leave"), cancelLabel: t("stay") }),
    [dirty, t],
  )

  useEffect(() => {
    if (!dirty) return

    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
    }

    // 링크(Next Link 포함)보다 먼저 받도록 문서 capture 단계에서 듣는다
    const onClick = (event: MouseEvent) => {
      if (event.defaultPrevented || event.button !== 0) return
      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
      const anchor = (event.target as Element | null)?.closest?.("a[href]") as HTMLAnchorElement | null
      if (!anchor || (anchor.target && anchor.target !== "_self") || anchor.hasAttribute("download")) return
      const url = new URL(anchor.href, window.location.href)
      if (url.origin !== window.location.origin) return
      if (url.pathname === window.location.pathname && url.search === window.location.search) return
      event.preventDefault()
      event.stopPropagation()
      void confirmLeave().then((leave) => {
        if (leave) router.push(url.pathname + url.search + url.hash)
      })
    }

    window.addEventListener("beforeunload", onBeforeUnload)
    document.addEventListener("click", onClick, true)
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload)
      document.removeEventListener("click", onClick, true)
    }
  }, [dirty, confirmLeave, router])

  return { confirmLeave }
}
