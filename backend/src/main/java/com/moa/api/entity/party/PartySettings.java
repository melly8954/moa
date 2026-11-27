package com.moa.api.entity.party;

/**
 * 파티를 만들거나 고칠 때 방장이 정하는 칸 (specs/008-party FR-002, FR-003, FR-021). 검증은 Service가 끝낸 값이다.
 *
 * @param gameCode 게임 코드 (GAME 그룹)
 * @param modeCode 게임 모드 코드 (없으면 null)
 * @param tierMinCode 최소 티어 코드 (없으면 null)
 * @param tierMaxCode 최대 티어 코드 (없으면 null)
 * @param title 제목
 * @param description 소개
 * @param capacity 모집 인원 (방장 포함)
 * @param positionEnabled 포지션 조건을 걸었는지
 */
public record PartySettings(
	String gameCode,
	String modeCode,
	String tierMinCode,
	String tierMaxCode,
	String title,
	String description,
	int capacity,
	boolean positionEnabled) {
}
