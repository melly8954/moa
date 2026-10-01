package com.moa.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 소프트 삭제 엔터티. 도메인 엔터티의 기본 부모다 (harness-psw 4.7).
 *
 * <p>상속하는 엔터티가 지킬 것 (ArchitectureTest가 검사한다)
 * <ul>
 *   <li>클래스에 {@code @SQLRestriction("deleted_at IS NULL")}을 붙인다. 삭제된 행을 조회에서 뺀다</li>
 *   <li>리포지토리는 SoftDeleteRepository를 상속한다. delete 메서드가 없어 물리 삭제를 할 수 없다</li>
 * </ul>
 * 삭제는 {@link #delete(Long)}로 한다. 행위자는 CurrentActor에서 얻는다.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseEntity {

	public static final String NOT_DELETED = "deleted_at IS NULL";

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by")
	private Long deletedBy;

	public void delete(Long actorId) {
		if (isDeleted()) {
			return;
		}
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = actorId;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
