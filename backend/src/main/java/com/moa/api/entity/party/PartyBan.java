package com.moa.api.entity.party;

import com.moa.api.dto.party.BanReason;
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
 * 파티 참가 제한 (specs/008-party FR-013). 내보내지거나 거절된 회원은 그 파티에 다시 들어오지 못한다.
 */
@Getter
@Entity
@Table(name = "tb_party_ban")
@HardDelete(reason = "파티가 해산돼도 남긴다. 지우는 기능이 없다 (research.md R12)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyBan extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "party_id", nullable = false)
	private Long partyId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BanReason reason;

	public PartyBan(Long partyId, Long userId, BanReason reason) {
		this.partyId = partyId;
		this.userId = userId;
		this.reason = reason;
	}
}
