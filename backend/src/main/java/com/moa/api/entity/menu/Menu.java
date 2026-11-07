package com.moa.api.entity.menu;

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
 * 메뉴 (선택 헬퍼 menu). 1단계(parentId 없음)는 그룹이거나 바로 가는 메뉴, 2단계는 그룹 아래 메뉴다.
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

	@Column(length = 200)
	private String path;

	@Column(length = 50)
	private String icon;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean enabled;

	/** 볼 때 필요한 권한 키. null이면 로그인한 사용자 모두 */
	@Column(length = 100)
	private String permission;

	public Menu(Long parentId, String name, String path, String icon, int sortOrder, String permission) {
		this.parentId = parentId;
		this.name = name;
		this.path = path;
		this.icon = icon;
		this.sortOrder = sortOrder;
		this.enabled = true;
		this.permission = permission;
	}

	public void modify(Long parentId, String name, String path, String icon, int sortOrder, boolean enabled,
		String permission) {
		this.parentId = parentId;
		this.name = name;
		this.path = path;
		this.icon = icon;
		this.sortOrder = sortOrder;
		this.enabled = enabled;
		this.permission = permission;
	}

	public void move(Long parentId, int sortOrder) {
		this.parentId = parentId;
		this.sortOrder = sortOrder;
	}
}
