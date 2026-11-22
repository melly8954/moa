"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useFieldErrorText } from "@/i18n/field-error"
import { useErrorMessages } from "@/lib/error-messages"
import { formatDateTime } from "@/lib/format"
import { ME_QUERY_KEY } from "@/lib/me"
import { type NicknameInput, nicknameSchema } from "@/lib/schemas/profile"
import type { MyProfile } from "@/types/profile"
import { PROFILE_QUERY_KEY } from "./profile-query"

// moa: 닉네임 바꾸기 (specs/009-mypage FR-003·FR-004·FR-005, US2). 프로필 저장과 따로 "닉네임 바꾸기" 단추로 저장한다
//   길이·글자는 zod가 먼저 막고, 예약어(V001)·태그 다 참(B201)·변경 제한(B202)은 서버 응답을 칸 아래에 보인다
//   nicknameChangeableAt이 있으면 "{date}부터 바꿀 수 있어요"를 미리 보이고 입력·단추를 막는다
//   B202(동시 저장·화면 우회)를 받으면 프로필을 다시 읽어 같은 안내를 보인다
//   성공하면 me·프로필을 다시 읽어 계정 바가 새로고침 없이 바뀐다 (SC-002)
export function NicknameField({ profile }: { profile: MyProfile }) {
  const t = useTranslations("profile")
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const [serverError, setServerError] = useState<string>()
  const form = useForm<NicknameInput>({
    resolver: zodResolver(nicknameSchema),
    defaultValues: { nickname: profile.nickname },
  })
  const value = useWatch({ control: form.control, name: "nickname" })
  const changeableAt = profile.nicknameChangeableAt
  const locked = changeableAt !== null
  const unchanged = value.normalize("NFC").trim() === profile.nickname

  const onSubmit = async ({ nickname }: NicknameInput) => {
    setServerError(undefined)
    try {
      const result = await api.v1.profile.changeNickname(nickname)
      form.reset({ nickname: result.nickname })
      toast.success(t("nicknameChanged"))
      // 프로필 키가 me 아래라 me를 무효화하면 둘 다 다시 읽는다
      await queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY })
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.code === "B202") {
        await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY })
        return
      }
      if (apiError.code === "B201") {
        form.setError("nickname", { message: "profile.nicknameTagsExhausted" })
        return
      }
      if (apiError.code === "V001" && apiError.fieldErrors.some((e) => e.field === "nickname")) {
        // 화면 규칙을 통과했으니 남은 것은 예약어다 (서버 문구는 화면에 쓰지 않는다)
        form.setError("nickname", { message: "profile.nicknameInvalid" })
        return
      }
      setServerError(describeError(apiError).message)
    }
  }

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
      <Controller
        name="nickname"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid || !!serverError}>
            <FieldLabel htmlFor="nickname">
              {t("nickname")} <span className="text-xs font-normal text-muted-foreground">{t("required")}</span>
            </FieldLabel>
            <div className="flex gap-2">
              <Input
                {...field}
                id="nickname"
                autoComplete="off"
                disabled={locked}
                aria-invalid={fieldState.invalid}
                aria-describedby="nickname-help"
              />
              <Button
                type="submit"
                variant="outline"
                className="shrink-0"
                disabled={locked || unchanged || form.formState.isSubmitting}
              >
                {t("changeNickname")}
              </Button>
            </div>
            <FieldDescription id="nickname-help">
              {changeableAt
                ? t("nicknameChangeableAt", { date: formatDateTime(changeableAt) })
                : `${t("nicknameHelp")} · ${t("nicknameCooldown")}`}
            </FieldDescription>
            {fieldState.invalid && <FieldError errors={fieldErrorText(fieldState.error)} />}
            {serverError && <FieldError errors={[{ message: serverError }]} />}
          </Field>
        )}
      />
    </form>
  )
}
