import type { TokenResponse } from "./api"
import type { Schema } from "./openapi"

// moa: 회원 탈퇴·복구 API의 응답·요청 (specs/018-member-withdrawal contracts/withdrawal-api.md). npm run gen:api의 생성 타입을 쓴다

// POST /api/v1/users/me/withdrawal
export type WithdrawalRequest = Schema<"WithdrawalRequest">

// purgeAt: 완전 삭제 예정 시각 (ISO-8601, 시간대 포함). 이때까지 다시 로그인하면 복구할 수 있다
export type WithdrawalResponse = Schema<"WithdrawalResponse">

// GET /api/v1/auth/recovery (복구 쿠키로 확인)
export type RecoveryResponse = Schema<"RecoveryResponse">

// POST /api/v1/auth/recovery: 로그인과 같은 응답
export type RecoverResponse = TokenResponse
