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
 * moa: 관리자가 화면에서 다룬다. 지우지 않고 사용 여부로 끈다 (specs/004-admin-codes FR-004·FR-008).
 * 그룹 코드는 만든 뒤 바꾸지 않는다
 */
@Getter
@Entity
@Table(name = "tb_code_group")
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

	/** moa: 사용 여부. 꺼지면 화면용 목록이 빈 목록이다 (FR-008) */
	@Column(nullable = false)
	private boolean enabled;

	/** 사용 중으로 만든다 */
	public CodeGroup(String groupCode, String name, @Nullable String description) {
		this.groupCode = groupCode;
		this.name = name;
		this.description = description;
		this.enabled = true;
	}

	public void modify(String name, @Nullable String description, boolean enabled) {
		this.name = name;
		this.description = description;
		this.enabled = enabled;
	}
}
