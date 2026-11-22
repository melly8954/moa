"use client"

import { useTranslations } from "next-intl"
import { Controller, useForm, useWatch } from "react-hook-form"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { DEFAULT_AVATAR, isAvatarKey } from "@/lib/avatars"
import { useCodes } from "@/lib/codes"
import type { DiscordVisibility, Gender, MyProfile, PlayTime } from "@/types/profile"
import { AvatarImage } from "./avatar-image"

const GENDERS: Gender[] = ["MALE", "FEMALE", "UNDISCLOSED"]
const DISCORD_VISIBILITIES: DiscordVisibility[] = ["PRIVATE", "FRIENDS", "MEMBERS"]
export const PLAY_TIME_GROUP = "PLAY_TIME"

// 폼 값. 빈 칸은 ""로 두고 저장할 때 null로 바꾼다
export type ProfileFormValues = {
  avatar: string
  bio: string
  birthYear: string
  gender: Gender | ""
  ageGenderPublic: boolean
  discordTag: string
  discordVisibility: DiscordVisibility
  playTimes: string[]
}

export function toFormValues(profile: MyProfile): ProfileFormValues {
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

// moa: 마이페이지 프로필 폼 (specs/009-mypage FR-001·FR-017, US1-AC1·AC4·AC8)
//   닉네임만 필수, 나머지는 모두 선택 표시. 비어 있는 칸은 "비어 있음"으로 보인다
//   저장(US3·US4·US6)과 닉네임 바꾸기(US2)는 다음 묶음에서 더한다. 게임 정보 영역은 US5 때 더한다
export function ProfileForm({ profile }: { profile: MyProfile }) {
  const t = useTranslations("profile")
  const playTimeCodes = useCodes(PLAY_TIME_GROUP)
  const form = useForm<ProfileFormValues>({ defaultValues: toFormValues(profile) })
  const options = playTimeOptions(playTimeCodes.data, profile.playTimes)
  const avatar = useWatch({ control: form.control, name: "avatar" })
  const nickname = profile.nicknameTag ? `${profile.nickname}#${profile.nicknameTag}` : profile.nickname

  return (
    <form noValidate>
      <FieldGroup>
        <Field>
          <FieldLabel>
            {t("avatar")} <Marker />
          </FieldLabel>
          <div>
            <AvatarImage
              avatar={avatar}
              alt={t(`avatarNames.${isAvatarKey(avatar) ? avatar : DEFAULT_AVATAR}`)}
              className="size-16"
            />
          </div>
        </Field>

        <Field>
          <FieldLabel htmlFor="nickname">
            {t("nickname")} <Marker required />
          </FieldLabel>
          <Input id="nickname" value={nickname} readOnly />
          <FieldDescription>{t("nicknameHelp")}</FieldDescription>
        </Field>

        <Controller
          name="bio"
          control={form.control}
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor="bio">
                {t("bio")} <Marker />
              </FieldLabel>
              <Textarea {...field} id="bio" maxLength={500} placeholder={t("empty")} />
            </Field>
          )}
        />

        <Controller
          name="birthYear"
          control={form.control}
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor="birthYear">
                {t("birthYear")} <Marker />
              </FieldLabel>
              <div className="flex items-center gap-2">
                <Input {...field} id="birthYear" inputMode="numeric" className="w-32" placeholder={t("empty")} />
                {profile.age !== null && (
                  <span className="text-sm text-muted-foreground">{t("age", { age: profile.age })}</span>
                )}
              </div>
            </Field>
          )}
        />

        <Controller
          name="gender"
          control={form.control}
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor="gender">
                {t("gender")} <Marker />
              </FieldLabel>
              <NativeSelect {...field} id="gender">
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
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor="discordTag">
                {t("discordTag")} <Marker />
              </FieldLabel>
              <Input {...field} id="discordTag" autoComplete="off" placeholder={t("empty")} />
              <FieldDescription>{t("discordTagHelp")}</FieldDescription>
            </Field>
          )}
        />

        <Controller
          name="discordVisibility"
          control={form.control}
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor="discordVisibility">
                {t("discordVisibility")} <Marker />
              </FieldLabel>
              <NativeSelect {...field} id="discordVisibility">
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
          render={({ field }) => (
            <FieldSet>
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
      </FieldGroup>
    </form>
  )
}
