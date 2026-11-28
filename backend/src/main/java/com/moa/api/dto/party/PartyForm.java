package com.moa.api.dto.party;

import java.util.List;

/**
 * 파티를 만들거나 고칠 때 방장이 보낸 칸 (specs/008-party FR-001~FR-003, FR-021~FR-022). 검증 전의 값이다.
 *
 * @param capacity 모집 인원. 포지션 조건이 있으면 쓰지 않고 자리의 합으로 정한다 (FR-022)
 * @param positions 모집할 포지션과 자리 수. 비었으면 포지션 조건이 없다
 * @param hostPositionCode 방장의 포지션. 포지션 조건이 있으면 꼭 있다. positions에 없으면 1자리를 더한다 (롤)
 */
public record PartyForm(
	String gameCode,
	String modeCode,
	String tierMinCode,
	String tierMaxCode,
	String title,
	String description,
	Integer capacity,
	List<PartyPositionSlot> positions,
	String hostPositionCode) {
}
