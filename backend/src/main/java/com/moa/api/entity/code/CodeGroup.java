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
 * 코드 그룹 (선택 헬퍼 code). 예: ORDER_STATUS(주문 상태), BANK(은행)
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

	@Column(name = "group_code", nullable = false, length = 50)
	private String groupCode;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	public CodeGroup(String groupCode, String name, String description) {
		this.groupCode = groupCode;
		this.name = name;
		this.description = description;
	}
}
