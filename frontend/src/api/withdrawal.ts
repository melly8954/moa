import type { RecoverResponse, RecoveryResponse, WithdrawalResponse } from "@/types/withdrawal"
import { http } from "./client"

// moa: 회원 탈퇴·복구 (specs/018-member-withdrawal contracts/withdrawal-api.md). api.v1.withdrawal로 부른다
//   withdraw: 닉네임이 다르면 V001(칸 nickname), 관리자 계정은 A201. 성공하면 서버가 리프레시 쿠키를 지운다
//   recovery·recover: 로그인에서 받은 복구 쿠키(HttpOnly)로 부른다. 쿠키가 없거나 맞지 않으면 A403
export const withdrawalApi = {
  withdraw: (nickname: string) =>
    http.post<WithdrawalResponse>("/api/v1/users/me/withdrawal", { nickname }).then((res) => res.data),
  recovery: () => http.get<RecoveryResponse>("/api/v1/auth/recovery", { skipAuth: true }).then((res) => res.data),
  recover: () =>
    http.post<RecoverResponse>("/api/v1/auth/recovery", undefined, { skipAuth: true }).then((res) => res.data),
}
