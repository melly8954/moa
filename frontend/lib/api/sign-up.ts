// 회원가입 API (FR-MEM-001). 가입 진행은 서버가 쿠키(sign_up_token)로 이어 간다
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { apiFetch, setAccessToken } from "./client"
import type { components } from "./schema"

type Schemas = components["schemas"]
export type SignUpState = Schemas["SignUpGetResponse"]
export type PhoneConflict = Schemas["PhoneConflict"]
export type EmailVerification = Schemas["EmailVerificationGetResponse"]
export type EmailConfirmation = Schemas["EmailConfirmationGetResponse"]
export type PhoneVerification = Schemas["PhoneVerificationGetResponse"]
export type TokenResponse = Schemas["AuthRefreshResponse"]

const SIGN_UP_KEY = ["sign-up", "current"] as const

/** 가입 진행 상태. 진행이 없거나 만료되면 A006 */
export function useSignUp() {
  return useQuery({
    queryKey: SIGN_UP_KEY,
    queryFn: () => apiFetch<SignUpState>("/api/v1/sign-ups/current"),
  })
}

export function useRequestEmailVerification() {
  return useMutation({
    mutationFn: (body: { email: string; password: string }) =>
      apiFetch<EmailVerification>("/api/v1/sign-ups/email-verifications", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  })
}

export function useConfirmEmail() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (token: string) =>
      apiFetch<EmailConfirmation>("/api/v1/sign-ups/email-confirmations", {
        method: "POST",
        body: JSON.stringify({ token }),
      }),
    onSuccess: (result) => {
      if (result.result === "SIGNED_IN" && result.accessToken) {
        setAccessToken(result.accessToken)
      }
      return queryClient.invalidateQueries({ queryKey: SIGN_UP_KEY })
    },
  })
}

export function useRequestPhoneCode() {
  return useMutation({
    mutationFn: (phoneNumber: string) =>
      apiFetch<PhoneVerification>("/api/v1/sign-ups/phone-verifications", {
        method: "POST",
        body: JSON.stringify({ phoneNumber }),
      }),
  })
}

export function useConfirmPhone() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { verificationCode: string; birthDate: string }) =>
      apiFetch<void>("/api/v1/sign-ups/phone-confirmations", { method: "POST", body: JSON.stringify(body) }),
    // 번호가 겹쳐 거부돼도 서버는 확인된 번호를 남기고 안내 정보를 진행 상태로 준다
    onSettled: () => queryClient.invalidateQueries({ queryKey: SIGN_UP_KEY }),
  })
}

export function useCreateMember() {
  return useMutation({
    mutationFn: (body: { nickname: string; termsAgreed: boolean; privacyAgreed: boolean }) =>
      apiFetch<TokenResponse>("/api/v1/members", { method: "POST", body: JSON.stringify(body) }),
    onSuccess: (tokens) => setAccessToken(tokens.accessToken ?? null),
  })
}

export function useCancelSignUp() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<void>("/api/v1/sign-ups/current", { method: "DELETE" }),
    onSuccess: () => queryClient.removeQueries({ queryKey: SIGN_UP_KEY }),
  })
}
