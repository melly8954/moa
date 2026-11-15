package com.moa.api.entity.menu;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuVisibility;
import com.moa.common.entity.SoftDeleteEntity;

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
 * 메뉴 (선택 헬퍼 menu). 1단계(parentId 없음)는 묶음 메뉴이거나 화면 메뉴, 2단계는 묶음 아래 화면 메뉴다.
 * 화면 메뉴는 프로그램을 가리킨다 (moa, specs/001-admin-menus FR-009).
 */
@Getter
@Entity
@Table(name = "menus")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(nullable = false, length = 50)
	private String name;

	/** 가리키는 프로그램 키 (moa, specs/001-admin-menus FR-009). null이면 묶음 메뉴다 */
	@Column(name = "program_key", length = 50)
	private String programKey;

	@Column(length = 50)
	private String icon;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean enabled;

	/** 메뉴 대상 (moa, specs/001-admin-menus FR-007). 하위 메뉴는 부모와 같다 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MenuAudience audience;

	/** 사용자 메뉴의 공개 범위 (moa, FR-007). 관리 메뉴·묶음 메뉴는 null */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private MenuVisibility visibility;

	public Menu(Long parentId, String name, String programKey, String icon, int sortOrder, MenuAudience audience,
		MenuVisibility visibility) {
		this.parentId = parentId;
		this.name = name;
		this.programKey = programKey;
		this.icon = icon;
		this.sortOrder = sortOrder;
		this.enabled = true;
		this.audience = audience;
		this.visibility = visibility;
	}

	public void modify(Long parentId, String name, String programKey, String icon, int sortOrder, boolean enabled,
		MenuAudience audience, MenuVisibility visibility) {
		this.parentId = parentId;
		this.name = name;
		this.programKey = programKey;
		this.icon = icon;
		this.sortOrder = sortOrder;
		this.enabled = enabled;
		this.audience = audience;
		this.visibility = visibility;
	}

	public void move(Long parentId, int sortOrder) {
		this.parentId = parentId;
		this.sortOrder = sortOrder;
	}
}
