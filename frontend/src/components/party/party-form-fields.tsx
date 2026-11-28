"use client"

import { useTranslations } from "next-intl"
import { Controller, type UseFormReturn, useWatch } from "react-hook-form"
import { CodeSelect } from "@/components/code/code-select"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { useFieldErrorText } from "@/i18n/field-error"
import { useCodes } from "@/lib/codes"
import {
  CAPACITY_MAX,
  CAPACITY_MIN,
  JOIN_POLICIES,
  type PartyInput,
  type PositionKind,
  modeGroupOf,
  positionCapacity,
  positionGroupOf,
  positionKindOf,
  tierGroupOf,
} from "@/lib/schemas/party"

const CAPACITIES = Array.from({ length: CAPACITY_MAX - CAPACITY_MIN + 1 }, (_, i) => CAPACITY_MIN + i)

// 파티 만들기·고치기 폼 칸 (specs/008-party 화면 "파티 만들기", T017·T036). 고치기 창(T033)도 이것을 쓴다
//   게임을 먼저 고르고, 고른 게임에 따라 모드·포지션·티어 칸이 바뀐다. 게임을 바꾸면 그 칸들을 비운다
//   포지션 조건을 켜면 인원은 자리 수의 합으로 정해지고 직접 고르지 않는다
//   showJoinPolicy: 고치기 창은 참가 방식을 따로 바꾸므로(PUT /join-policy) 숨긴다
export function PartyFormFields({
  form,
  idPrefix = "party",
  showJoinPolicy = true,
}: {
  form: UseFormReturn<PartyInput>
  idPrefix?: string
  showJoinPolicy?: boolean
}) {
  const t = useTranslations("party.create")
  const tp = useTranslations("party.policy")
  const fieldErrorText = useFieldErrorText()
  const gameCode = useWatch({ control: form.control, name: "gameCode" })
  const positionEnabled = useWatch({ control: form.control, name: "positionEnabled" })
  const kind = gameCode ? positionKindOf(gameCode) : undefined
  const id = (name: string) => `${idPrefix}-${name}`

  const changeGame = (next: string) => {
    form.setValue("gameCode", next, { shouldDirty: true, shouldValidate: form.formState.isSubmitted })
    form.setValue("modeCode", "", { shouldDirty: true })
    form.setValue("tierMinCode", "", { shouldDirty: true })
    form.setValue("tierMaxCode", "", { shouldDirty: true })
    form.setValue("hostPositionCode", "", { shouldDirty: true })
    form.setValue("slots", {}, { shouldDirty: true })
    if (!positionKindOf(next)) form.setValue("positionEnabled", false, { shouldDirty: true })
  }

  return (
    <FieldGroup>
      <Controller
        name="gameCode"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("game")}>{t("game")}</FieldLabel>
            <CodeSelect
              groupCode="GAME"
              id={id("game")}
              value={field.value}
              onChange={changeGame}
              onBlur={field.onBlur}
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("game-error") : undefined}
            />
            {fieldState.invalid && <FieldError id={id("game-error")} errors={fieldErrorText(fieldState.error)} />}
          </Field>
        )}
      />

      {gameCode && (
        <Controller
          name="modeCode"
          control={form.control}
          render={({ field }) => (
            <Field>
              <FieldLabel htmlFor={id("mode")}>{t("mode")}</FieldLabel>
              <CodeSelect
                groupCode={modeGroupOf(gameCode)}
                id={id("mode")}
                value={field.value}
                onChange={field.onChange}
                placeholder={t("noCondition")}
              />
            </Field>
          )}
        />
      )}

      {gameCode && kind && (
        <Controller
          name="positionEnabled"
          control={form.control}
          render={({ field }) => (
            <Field orientation="horizontal">
              <Switch
                id={id("position-enabled")}
                aria-label={t("positionEnabled")}
                checked={field.value}
                onCheckedChange={(checked) => field.onChange(checked)}
              />
              <FieldLabel htmlFor={id("position-enabled")}>{t("positionEnabled")}</FieldLabel>
            </Field>
          )}
        />
      )}

      {gameCode && kind && positionEnabled && <PositionFields form={form} gameCode={gameCode} kind={kind} id={id} />}

      {!(kind && positionEnabled) && (
        <Controller
          name="capacity"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor={id("capacity")}>{t("capacity")}</FieldLabel>
              <NativeSelect
                id={id("capacity")}
                value={String(field.value)}
                onChange={(e) => field.onChange(Number(e.target.value))}
                aria-invalid={fieldState.invalid}
              >
                {CAPACITIES.map((n) => (
                  <NativeSelectOption key={n} value={String(n)}>
                    {t("people", { count: n })}
                  </NativeSelectOption>
                ))}
              </NativeSelect>
              {fieldState.invalid && <FieldError errors={fieldErrorText(fieldState.error)} />}
            </Field>
          )}
        />
      )}

      {gameCode && (
        <div className="grid grid-cols-2 gap-3">
          <Controller
            name="tierMinCode"
            control={form.control}
            render={({ field }) => (
              <Field>
                <FieldLabel htmlFor={id("tier-min")}>{t("tierMin")}</FieldLabel>
                <CodeSelect
                  groupCode={tierGroupOf(gameCode)}
                  id={id("tier-min")}
                  value={field.value}
                  onChange={field.onChange}
                  placeholder={t("noCondition")}
                />
              </Field>
            )}
          />
          <Controller
            name="tierMaxCode"
            control={form.control}
            render={({ field }) => (
              <Field>
                <FieldLabel htmlFor={id("tier-max")}>{t("tierMax")}</FieldLabel>
                <CodeSelect
                  groupCode={tierGroupOf(gameCode)}
                  id={id("tier-max")}
                  value={field.value}
                  onChange={field.onChange}
                  placeholder={t("noCondition")}
                />
              </Field>
            )}
          />
        </div>
      )}

      <Controller
        name="title"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("title")}>{t("fieldTitle")}</FieldLabel>
            <Input
              {...field}
              id={id("title")}
              maxLength={50}
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("title-error") : undefined}
            />
            {fieldState.invalid && <FieldError id={id("title-error")} errors={fieldErrorText(fieldState.error)} />}
          </Field>
        )}
      />

      <Controller
        name="description"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("description")}>{t("fieldDescription")}</FieldLabel>
            <Textarea
              {...field}
              id={id("description")}
              rows={4}
              maxLength={1000}
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("description-error") : undefined}
            />
            {fieldState.invalid && (
              <FieldError id={id("description-error")} errors={fieldErrorText(fieldState.error)} />
            )}
          </Field>
        )}
      />

      {showJoinPolicy && (
        <Controller
          name="joinPolicy"
          control={form.control}
          render={({ field }) => (
            <FieldSet>
              <FieldLegend variant="label">{t("joinPolicy")}</FieldLegend>
              <RadioGroup value={field.value} onValueChange={(value) => field.onChange(value)}>
                {JOIN_POLICIES.map((policy) => (
                  <Field key={policy} orientation="horizontal">
                    <RadioGroupItem value={policy} id={id(`policy-${policy}`)} aria-label={tp(policy)} />
                    <div className="flex flex-col gap-0.5">
                      <FieldLabel htmlFor={id(`policy-${policy}`)}>{tp(policy)}</FieldLabel>
                      <FieldDescription>{tp(`${policy}_HINT`)}</FieldDescription>
                    </div>
                  </Field>
                ))}
              </RadioGroup>
            </FieldSet>
          )}
        />
      )}
    </FieldGroup>
  )
}

// 포지션 칸 (T036)
//   롤(single): 내 포지션 하나 + 모집할 포지션 체크 (포지션마다 1자리)
//   발로란트·오버워치(multi): 내 포지션 하나 + 포지션마다 자리 수 (내 포지션은 1자리 이상)
function PositionFields({
  form,
  gameCode,
  kind,
  id,
}: {
  form: UseFormReturn<PartyInput>
  gameCode: string
  kind: PositionKind
  id: (name: string) => string
}) {
  const t = useTranslations("party.create")
  const fieldErrorText = useFieldErrorText()
  const positions = useCodes(positionGroupOf(gameCode)).data ?? []
  const hostPositionCode = useWatch({ control: form.control, name: "hostPositionCode" })
  const slots = useWatch({ control: form.control, name: "slots" })
  const total = positionCapacity({ gameCode, hostPositionCode, slots })
  // slots는 객체 칸이라 오류가 칸 전체(message)에 붙는다
  const slotsMessage = form.formState.errors.slots?.message as string | undefined

  const setSlot = (code: string, n: number) =>
    form.setValue("slots", { ...slots, [code]: n }, { shouldDirty: true, shouldValidate: form.formState.isSubmitted })

  return (
    <>
      <Controller
        name="hostPositionCode"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={id("host-position")}>{t("hostPosition")}</FieldLabel>
            <CodeSelect
              groupCode={positionGroupOf(gameCode)}
              id={id("host-position")}
              value={field.value}
              onChange={(value) => {
                field.onChange(value)
                if (form.formState.isSubmitted) void form.trigger("slots")
              }}
              aria-invalid={fieldState.invalid}
              aria-describedby={fieldState.invalid ? id("host-position-error") : undefined}
            />
            {fieldState.invalid && (
              <FieldError id={id("host-position-error")} errors={fieldErrorText(fieldState.error)} />
            )}
          </Field>
        )}
      />
      <FieldSet data-invalid={!!slotsMessage}>
        <FieldLegend variant="label">{t(kind === "single" ? "recruitPositions" : "positionSlots")}</FieldLegend>
        <FieldDescription>{t(kind === "single" ? "recruitPositionsHint" : "positionSlotsHint")}</FieldDescription>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {positions.map((p) => {
            const isHost = p.code === hostPositionCode
            const value = slots[p.code] ?? 0
            return kind === "single" ? (
              <Field key={p.code} orientation="horizontal">
                <Checkbox
                  id={id(`slot-${p.code}`)}
                  aria-label={p.name}
                  checked={isHost || value > 0}
                  disabled={isHost}
                  onCheckedChange={(checked) => setSlot(p.code, checked ? 1 : 0)}
                />
                <FieldLabel htmlFor={id(`slot-${p.code}`)}>
                  {isHost ? t("hostPositionMark", { name: p.name }) : p.name}
                </FieldLabel>
              </Field>
            ) : (
              <Field key={p.code}>
                <FieldLabel htmlFor={id(`slot-${p.code}`)}>
                  {isHost ? t("hostPositionMark", { name: p.name }) : p.name}
                </FieldLabel>
                <Input
                  id={id(`slot-${p.code}`)}
                  type="number"
                  inputMode="numeric"
                  min={isHost ? 1 : 0}
                  max={10}
                  value={isHost ? Math.max(value, 1) : value}
                  onChange={(e) => setSlot(p.code, Math.max(0, Math.min(10, Number(e.target.value) || 0)))}
                />
              </Field>
            )
          })}
        </div>
        <p className="text-sm" aria-live="polite">
          {t("autoCapacity", { count: total })}
        </p>
        {slotsMessage && <FieldError errors={fieldErrorText({ type: "custom", message: slotsMessage })} />}
      </FieldSet>
    </>
  )
}
