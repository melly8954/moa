package com.moa.api.dto.party;

import java.util.Map;
import java.util.Set;

/**
 * 파티가 고를 수 있는 공통코드 (specs/008-party FR-002, research R7). Application이 공통코드에서 읽어 Service에 넘긴다.
 * 꺼진 그룹과 없는 그룹은 빈 목록이다.
 *
 * @param games 사용 중인 게임 코드 (GAME)
 * @param modes 그 게임의 모드 (&lt;게임&gt;_MODE)
 * @param positions 그 게임의 포지션 (&lt;게임&gt;_POSITION). 비었으면 포지션을 고를 수 없는 게임이다
 * @param tiers 그 게임의 티어와 정렬 순서 (&lt;게임&gt;_TIER). 순서가 작을수록 낮은 티어다
 */
public record PartyCodeBook(Set<String> games, Set<String> modes, Set<String> positions, Map<String, Integer> tiers) {
}
