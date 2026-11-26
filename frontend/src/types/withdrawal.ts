import type { TokenResponse } from "./api"

// moa: 회원 탈퇴·복구 API의 응답·요청 (specs/018-member-withdrawal contracts/withdrawal-api.md)
// 백엔드가 먼저 생겼지만 생성 타입(schema.d.ts)은 T044에서 npm run gen:api로 다시 만든다. 그때 Schema<"…">로 바꾼다

// POST /api/v1/users/me/withdrawal
export type WithdrawalRequest = { nickname: string }

// purgeAt: 완전 삭제 예정 시각 (ISO-8601, 시간대 포함). 이때까지 다시 로그인하면 복구할 수 있다
export type WithdrawalResponse = { purgeAt: string }

// GET /api/v1/auth/recovery (복구 쿠키로 확인)
export type RecoveryResponse = { purgeAt: string }

// POST /api/v1/auth/recovery: 로그인과 같은 응답
export type RecoverResponse = TokenResponse
