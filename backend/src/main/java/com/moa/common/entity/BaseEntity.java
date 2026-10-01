package com.moa.common.entity;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import com.moa.common.audit.CurrentActor;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

/**
 * 시각과 행위자 감사 필드를 가진 엔터티. 행위자는 비지 않는다 (CurrentActor, requireActor).
 * 도메인 엔터티는 보통 SoftDeleteEntity를 상속한다. 이 클래스를 직접 상속하면 @HardDelete를 붙인다.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

	@CreatedBy
	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", nullable = false)
	private Long updatedBy;

	/** 감사 리스너가 행위자를 채운 뒤에 불린다. 행위자가 비면 저장하지 않는다 */
	@PrePersist
	@PreUpdate
	void requireActor() {
		if (createdBy == null || updatedBy == null) {
			throw new IllegalStateException(CurrentActor.MISSING_ACTOR);
		}
	}
}
