package com.moa.api.application.v1.loginhistory.query;

import java.time.LocalDate;

/**
 * 접속 이력 조회 조건. 모두 선택이고, 함께 주면 AND로 묶는다.
 *
 * @param userId 그 사용자의 이력 (실패 이력에는 사용자 ID가 없으므로 성공 로그인만 걸린다)
 * @param loginId 입력한 아이디와 정확히 일치
 * @param success true면 성공만, false면 실패만. null이면 전체
 * @param from 이 날짜부터 (포함)
 * @param to 이 날짜까지 (포함, 그날 끝까지)
 */
public record LoginHistoryQuery(Long userId, String loginId, Boolean success, LocalDate from, LocalDate to) {
}
