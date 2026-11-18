package com.moa.api.entity.code;

import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import com.moa.common.entity.SoftDeleteEntity;

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
 * 코드 그룹 (선택 헬퍼 code). 예: GAME_GENRE(게임 장르).
 * moa: 관리자가 화면에서 다룬다. 기능이 코드에서 읽는 그룹은 마이그레이션으로 넣은 시스템 그룹이고 지울 수 없다
 * (specs/004-admin-codes FR-004·FR-008). 그룹 코드는 만든 뒤 바꾸지 않는다
 */
@Getter
@Entity
@Table(name = "code_groups")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_code", nullable = false, length = 50, updatable = false)
	private String groupCode;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private @Nullable String description;

	/** moa: 쓰는 곳 */
	@Column(name = "used_by", length = 200)
	private @Nullable String usedBy;

	/** moa: 기능이 코드에서 읽는 그룹. 마이그레이션으로만 켠다 (FR-008) */
	@Column(name = "`system`", nullable = false, updatable = false)
	private boolean system;

	/** 관리자가 만드는 일반 그룹 */
	public CodeGroup(String groupCode, String name, @Nullable String description, @Nullable String usedBy) {
		this.groupCode = groupCode;
		this.name = name;
		this.description = description;
		this.usedBy = usedBy;
		this.system = false;
	}

	public void modify(String name, @Nullable String description, @Nullable String usedBy) {
		this.name = name;
		this.description = description;
		this.usedBy = usedBy;
	}
}
