"use client"

import { useTranslations } from "next-intl"
import { useCallback } from "react"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { useErrorMessages } from "@/lib/error-messages"
import { imageFileProblem } from "@/lib/rich-text"

// 본문 이미지 올리기 오류 → 화면 문구. V001(형식·크기·빈 파일)은 이미지 규칙으로 알린다
export function useImageUploadErrorText() {
  const t = useTranslations("richText")
  const { describeError } = useErrorMessages()
  return useCallback(
    (error: unknown) => (toApiError(error).code === "V001" ? t("imageRejected") : describeError(error).message),
    [t, describeError],
  )
}

// 본문 이미지를 올리고 img src에 넣을 주소를 돌려준다. 올리지 못하면 알림을 띄우고 null
//   형식·크기는 올리기 전에 먼저 본다 (서버도 파일 내용으로 다시 본다)
export function useImageUpload() {
  const t = useTranslations("richText")
  const errorText = useImageUploadErrorText()
  return useCallback(
    async (file: File): Promise<string | null> => {
      const problem = imageFileProblem(file)
      if (problem) {
        toast.error(t(problem))
        return null
      }
      try {
        return (await api.v1.richText.uploadImage(file)).url
      } catch (error) {
        toast.error(errorText(error))
        return null
      }
    },
    [t, errorText],
  )
}
