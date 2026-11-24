// 배너·팝업이 같이 쓰는 값 (specs/016-banner-popup contracts/admin-api.md)

// 상태: 켜짐과 노출 기간으로 서버가 정한다 (FR-006)
export type ExposureStatus = "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED"

// 목록 상태 거르기. 비면 끝난 것만 빼고, ALL이면 모두 (research R4)
export type ExposureStatusFilter = ExposureStatus | "ALL"

// 모두 선택
export type ExposureQuery = { keyword?: string; status?: string }
