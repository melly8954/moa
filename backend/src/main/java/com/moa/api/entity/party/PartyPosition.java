package com.moa.api.entity.party;

import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파티 포지션 자리 (specs/008-party FR-022). 방장의 자리도 여기에 센다.
 */
@Getter
@Entity
@Table(name = "tb_party_position")
@HardDelete(reason = "파티 설정의 일부이고 이력이 필요 없다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyPosition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "party_id", nullable = false)
	private Long partyId;

	@Column(name = "position_code", nullable = false, length = 50)
	private String positionCode;

	@Column(nullable = false, columnDefinition = "TINYINT")
	private int slots;

	public PartyPosition(Long partyId, String positionCode, int slots) {
		this.partyId = partyId;
		this.positionCode = positionCode;
		this.slots = slots;
	}

	public void changeSlots(int slots) {
		this.slots = slots;
	}
}
