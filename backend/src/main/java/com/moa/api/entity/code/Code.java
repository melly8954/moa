package com.moa.api.entity.code;

import org.hibernate.annotations.SQLRestriction;

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
 * 코드 (선택 헬퍼 code). 그룹 안에서 code가 겹치지 않는다. 화면 목록은 sortOrder 순이다.
 */
@Getter
@Entity
@Table(name = "codes")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Code extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_code", nullable = false, length = 50)
	private String groupCode;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean enabled;

	public Code(String groupCode, String code, String name, int sortOrder) {
		this.groupCode = groupCode;
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
		this.enabled = true;
	}

	public void modify(String name, int sortOrder, boolean enabled) {
		this.name = name;
		this.sortOrder = sortOrder;
		this.enabled = enabled;
	}
}
