package com.moa.common.purgelog;

import com.moa.common.entity.BaseTimeEntity;
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
 * 개인정보 파기 이력 (specs/013-purge-log). 개인정보는 갖지 않는다 (FR-002). 저장 뒤 고치지 않는다.
 * 파기 일시는 created_at이다.
 */
@Getter
@Entity
@Table(name = "tb_purge_log")
@HardDelete(reason = "보관 기간(1년)이 지난 파기 이력은 정리기가 지운다. 개인정보가 없어 기록 없이 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurgeLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PurgeTarget target;

	@Enumerated(EnumType.STRING)
	@Column(name = "purge_method", nullable = false, length = 20)
	private PurgeMethod purgeMethod;

	@Column(name = "purged_count", nullable = false)
	private int purgedCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 20)
	private PurgeActor actorType;

	@Column(name = "actor_user_id")
	private Long actorUserId;

	@Column(name = "target_user_id")
	private Long targetUserId;

	public PurgeLog(PersonalDataPurgedEvent event) {
		if (event.count() < 1) {
			throw new IllegalArgumentException("파기 건수는 1 이상이어야 합니다: " + event.count());
		}
		this.target = event.target();
		this.purgeMethod = event.method();
		this.purgedCount = event.count();
		this.actorType = event.actor();
		this.actorUserId = event.actorUserId();
		this.targetUserId = event.targetUserId();
	}
}
