"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useQueryClient } from "@tanstack/react-query"
import { useTranslations } from "next-intl"
import { useMemo, useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { api } from "@/api"
import { toApiError } from "@/api/error"
import { useUnsavedChanges } from "@/components/kit/use-unsaved-changes"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { useFieldErrorText } from "@/i18n/field-error"
import { DEFAULT_AVATAR, isAvatarKey } from "@/lib/avatars"
import { useCodes } from "@/lib/codes"
import { useErrorMessages } from "@/lib/error-messages"
import { ME_QUERY_KEY } from "@/lib/me"
import { BIO_MAX, BIRTH_YEAR_RANGE, MIN_BIRTH_YEAR, type ProfileInput, profileSchema } from "@/lib/schemas/profile"
import type { DiscordVisibility, Gender, MyProfile, PlayTime, ProfileSaveRequest } from "@/types/profile"
import { AvatarImage } from "./avatar-image"
import { AvatarPicker } from "./avatar-picker"
import { NicknameField } from "./nickname-field"

const GENDERS: Gender[] = ["MALE", "FEMALE", "UNDISCLOSED"]
const DISCORD_VISIBILITIES: DiscordVisibility[] = ["PRIVATE", "FRIENDS", "MEMBERS"]
export const PLAY_TIME_GROUP = "PLAY_TIME"

// 서버 V001 칸 오류 → 그 칸에 보일 문구 키. 문구가 없는 칸은 칸만 표시하고 폼 위에 공통 안내를 보인다
const SERVER_FIELD_MESSAGES: Partial<Record<keyof ProfileInput, string>> = {
  bio: "profile.bioMax",
  birthYear: BIRTH_YEAR_RANGE,
  discordTag: "profile.discordTagFormat",
}

// 폼 값. 빈 칸은 ""로 두고 저장할 때 null로 바꾼다
export function toFormValues(profile: MyProfile): ProfileInput {
  return {
    avatar: profile.avatar,
    bio: profile.bio ?? "",
    birthYear: profile.birthYear?.toString() ?? "",
    gender: profile.gender ?? "",
    ageGenderPublic: profile.ageGenderPublic,
    discordTag: profile.discordTag ?? "",
    discordVisibility: profile.discordVisibility,
    playTimes: profile.playTimes.map((p) => p.code),
  }
}

// 모든 칸을 보낸다. 빈 값은 null, 빈 목록은 "모두 지움" (US3-AC5, US6-AC2)
export function toSaveRequest(values: ProfileInput): ProfileSaveRequest {
  const discordTag = values.discordTag.trim()
  return {
    avatar: values.avatar,
    bio: values.bio.trim() === "" ? null : values.bio,
    birthYear: values.birthYear.trim() === "" ? null : Number(values.birthYear),
    gender: values.gender === "" ? null : values.gender,
    ageGenderPublic: values.ageGenderPublic,
    discordTag: discordTag === "" ? null : discordTag,
    discordVisibility: values.discordVisibility,
    playTimes: [...new Set(values.playTimes)],
  }
}

// 고를 수 있는 시간대: 켜진 공통코드 + 이미 저장된 꺼진 코드(이름과 함께 그대로 보인다, spec Edge Cases)
function playTimeOptions(enabled: { code: string; name: string }[] | undefined, saved: PlayTime[]): PlayTime[] {
  const options = (enabled ?? []).map(({ code, name }) => ({ code, name }))
  for (const p of saved) if (!options.some((o) => o.code === p.code)) options.push(p)
  return options
}

function Marker({ required }: { required?: boolean }) {
  const t = useTranslations("profile")
  return <span className="text-xs font-normal text-muted-foreground">{t(required ? "required" : "optional")}</span>
}

// moa: 마이페이지 프로필 폼 (specs/009-mypage FR-001·FR-005~FR-009·FR-012·FR-017, US1~US4·US6)
//   머리: 아바타와 닉네임#태그(태그가 없으면 닉네임만), 아바타 고르기
//   닉네임은 따로 "닉네임 바꾸기"로 저장하고(NicknameField), 나머지는 "저장" 한 번으로 모두 보낸다
//   닉네임만 필수, 나머지는 모두 선택 표시. 비어 있는 칸은 "비어 있음"으로 보인다
//   저장이 끝나면 me·프로필을 다시 읽어 계정 바 아바타가 새로고침 없이 바뀐다 (FR-005)
//   게임 정보 영역은 US5 때 더한다
export function ProfileForm({ profile }: { profile: MyProfile }) {
  const t = useTranslations("profile")
  const queryClient = useQueryClient()
  const fieldErrorText = useFieldErrorText()
  const { describeError } = useErrorMessages()
  const playTimeCodes = useCodes(PLAY_TIME_GROUP)
  const thisYear = new Date().getFullYear()
  const schema = useMemo(() => profileSchema(thisYear), [thisYear])
  const [formError, setFormError] = useState<string>()
  const form = useForm<ProfileInput>({ resolver: zodResolver(schema), defaultValues: toFormValues(profile) })
  useUnsavedChanges(form.formState.isDirty)
  const avatar = useWatch({ control: form.control, name: "avatar" })
  const bio = useWatch({ control: form.control, name: "bio" })
  const options = playTimeOptions(playTimeCodes.data, profile.playTimes)
  const displayName = profile.nicknameTag ? `${profile.nickname}#${profile.nicknameTag}` : profile.nickname

  // 출생 연도 문구는 범위를 끼운다. 나머지는 키를 그대로 문구로 바꾼다
  const errorText = (error: Parameters<typeof fieldErrorText>[0]) =>
    error?.message === BIRTH_YEAR_RANGE
      ? [{ message: t("birthYearRange", { min: MIN_BIRTH_YEAR, max: thisYear }) }]
      : fieldErrorText(error)

  const onSubmit = async (values: ProfileInput) => {
    setFormError(undefined)
    try {
      const saved = await api.v1.profile.save(toSaveRequest(values))
      form.reset(toFormValues(saved))
      toast.success(t("saved"))
      // 프로필 키가 me 아래라 me를 무효화하면 둘 다 다시 읽는다
      await queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY })
    } catch (error) {
      const apiError = toApiError(error)
      let unknownField = false
      for (const fieldError of apiError.fieldErrors) {
        const name = fieldError.field as keyof ProfileInput
        if (!(name in values)) continue
        const message = SERVER_FIELD_MESSAGES[name]
        form.setError(name, { message: message ?? "" })
        if (!message) unknownField = true
      }
      if (apiError.fieldErrors.length === 0 || unknownField) setFormError(describeError(apiError).message)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <AvatarImage
          avatar={avatar}
          alt={t(`avatarNames.${isAvatarKey(avatar) ? avatar : DEFAULT_AVATAR}`)}
          className="size-16"
        />
        <div className="flex min-w-0 flex-col gap-1.5">
          <span className="truncate text-lg font-semibold">{displayName}</span>
          <div className="flex items-center gap-2">
            <AvatarPicker value={avatar} onChange={(key) => form.setValue("avatar", key, { shouldDirty: true })} />
            <Marker />
          </div>
        </div>
      </div>

      <NicknameField profile={profile} />

      <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
        <FieldGroup>
          {formError && (
            <Alert variant="destructive">
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}

          <Controller
            name="bio"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="bio">
                  {t("bio")} <Marker />
                </FieldLabel>
                <Textarea
                  {...field}
                  id="bio"
                  maxLength={BIO_MAX}
                  placeholder={t("empty")}
                  aria-invalid={fieldState.invalid}
                />
                <FieldDescription className="text-right tabular-nums">
                  {bio.length}/{BIO_MAX}
                </FieldDescription>
                {fieldState.invalid && <FieldError errors={errorText(fieldState.error)} />}
              </Field>
            )}
          />

          <Controller
            name="birthYear"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="birthYear">
                  {t("birthYear")} <Marker />
                </FieldLabel>
                <div className="flex items-center gap-2">
                  <Input
                    {...field}
                    id="birthYear"
                    inputMode="numeric"
                    maxLength={4}
                    className="w-32"
                    placeholder={t("empty")}
                    aria-invalid={fieldState.invalid}
                  />
                  {/^\d{4}$/.test(field.value) && !fieldState.invalid && (
                    <span className="text-sm text-muted-foreground">
                      {t("age", { age: thisYear - Number(field.value) })}
                    </span>
                  )}
                </div>
                {fieldState.invalid && <FieldError errors={errorText(fieldState.error)} />}
              </Field>
            )}
          />

          <Controller
            name="gender"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="gender">
                  {t("gender")} <Marker />
                </FieldLabel>
                <NativeSelect {...field} id="gender" aria-invalid={fieldState.invalid}>
                  <NativeSelectOption value="">{t("genderNone")}</NativeSelectOption>
                  {GENDERS.map((g) => (
                    <NativeSelectOption key={g} value={g}>
                      {t(`genders.${g}`)}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
              </Field>
            )}
          />

          <Controller
            name="ageGenderPublic"
            control={form.control}
            render={({ field }) => (
              <Field orientation="horizontal">
                <Switch id="ageGenderPublic" checked={field.value} onCheckedChange={field.onChange} />
                <FieldContent>
                  <FieldLabel htmlFor="ageGenderPublic">
                    {t("ageGenderPublic")} <Marker />
                  </FieldLabel>
                  <FieldDescription>{t("ageGenderPublicHint")}</FieldDescription>
                </FieldContent>
              </Field>
            )}
          />

          <Controller
            name="discordTag"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="discordTag">
                  {t("discordTag")} <Marker />
                </FieldLabel>
                <Input
                  {...field}
                  id="discordTag"
                  autoComplete="off"
                  autoCapitalize="none"
                  placeholder={t("empty")}
                  aria-invalid={fieldState.invalid}
                />
                <FieldDescription>{t("discordTagHelp")}</FieldDescription>
                {fieldState.invalid && <FieldError errors={errorText(fieldState.error)} />}
              </Field>
            )}
          />

          <Controller
            name="discordVisibility"
            control={form.control}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="discordVisibility">
                  {t("discordVisibility")} <Marker />
                </FieldLabel>
                <NativeSelect {...field} id="discordVisibility" aria-invalid={fieldState.invalid}>
                  {DISCORD_VISIBILITIES.map((v) => (
                    <NativeSelectOption key={v} value={v}>
                      {t(`discordVisibilities.${v}`)}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
              </Field>
            )}
          />

          <Controller
            name="playTimes"
            control={form.control}
            render={({ field, fieldState }) => (
              <FieldSet data-invalid={fieldState.invalid}>
                <FieldLegend variant="label">
                  {t("playTimes")} <Marker />
                </FieldLegend>
                <div className="flex flex-wrap gap-x-4 gap-y-2">
                  {options.map((option) => (
                    <Field key={option.code} orientation="horizontal" className="w-auto">
                      <Checkbox
                        id={`playTime-${option.code}`}
                        checked={field.value.includes(option.code)}
                        onCheckedChange={(checked) =>
                          field.onChange(
                            checked ? [...field.value, option.code] : field.value.filter((c) => c !== option.code),
                          )
                        }
                      />
                      <FieldLabel htmlFor={`playTime-${option.code}`} className="font-normal">
                        {option.name}
                      </FieldLabel>
                    </Field>
                  ))}
                </div>
              </FieldSet>
            )}
          />

          <div className="flex justify-end">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {t("save")}
            </Button>
          </div>
        </FieldGroup>
      </form>
    </div>
  )
}
