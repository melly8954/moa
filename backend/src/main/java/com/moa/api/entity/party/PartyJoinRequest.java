package com.moa.api.entity.party;

import java.time.LocalDateTime;

import com.moa.api.dto.party.JoinRequestStatus;
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
 * 승인 파티의 참가 신청 (specs/008-party US5). 한 파티에 대기 신청은 하나다 (가상 열 pending_key 유니크).
 * 이름이 PartyJoinRequest인 것은 Application 계층의 PartyApplication과 겹치지 않게 하려는 것이다.
 */
@Getter
@Entity
@Table(name = "tb_party_join_request")
@HardDelete(reason = "처리된 신청은 상태로 남기고, 파티가 해산되면 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyJoinRequest extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "party_id", nullable = false)
	private Long partyId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "position_code", length = 50)
	private String positionCode;

	@Column(name = "tier_code", length = 50)
	private String tierCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JoinRequestStatus status;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	public PartyJoinRequest(Long partyId, Long userId, String positionCode, String tierCode) {
		this.partyId = partyId;
		this.userId = userId;
		this.positionCode = positionCode;
		this.tierCode = tierCode;
		this.status = JoinRequestStatus.PENDING;
	}

	public boolean isPending() {
		return status == JoinRequestStatus.PENDING;
	}

	public void approve(LocalDateTime now) {
		decide(JoinRequestStatus.APPROVED, now);
	}

	public void reject(LocalDateTime now) {
		decide(JoinRequestStatus.REJECTED, now);
	}

	public void cancel(LocalDateTime now) {
		decide(JoinRequestStatus.CANCELLED, now);
	}

	private void decide(JoinRequestStatus status, LocalDateTime now) {
		this.status = status;
		this.decidedAt = now;
	}
}
