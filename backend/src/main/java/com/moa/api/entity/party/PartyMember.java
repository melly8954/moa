package com.moa.api.entity.party;

import java.time.LocalDateTime;

import com.moa.api.dto.party.PartyRole;
import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파티 멤버 (specs/008-party). 회원은 한 번에 한 파티에만 속한다 (FR-026, 유니크 user_id).
 */
@Getter
@Entity
@Table(name = "tb_party_member")
@HardDelete(reason = "나가거나 내보내지면 행을 지운다. 이력은 채팅의 안내 메시지로 남는다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "party_id", nullable = false)
	private Long partyId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartyRole role;

	@Column(name = "position_code", length = 50)
	private String positionCode;

	@Column(name = "tier_code", length = 50)
	private String tierCode;

	/** 방장을 잇는 순서다. 같으면 id가 작은 쪽이 먼저다 (FR-010) */
	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	public PartyMember(Long partyId, Long userId, PartyRole role, String positionCode, String tierCode,
		LocalDateTime joinedAt) {
		this.partyId = partyId;
		this.userId = userId;
		this.role = role;
		this.positionCode = positionCode;
		this.tierCode = tierCode;
		this.joinedAt = joinedAt;
	}

	public boolean isHost() {
		return role == PartyRole.HOST;
	}

	public void promoteToHost() {
		this.role = PartyRole.HOST;
	}

	/** 방장이 포지션 조건을 고치면서 자기 포지션을 바꾼다 (US7-AC1, US10-AC10) */
	public void changePosition(String positionCode) {
		this.positionCode = positionCode;
	}
}
