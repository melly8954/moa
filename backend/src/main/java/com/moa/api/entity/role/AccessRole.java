package com.moa.api.entity.role;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;

import com.moa.common.entity.SoftDeleteEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업무 역할 (선택 헬퍼 rbac). 권한 키를 묶는다. 시스템 역할(common.security.Role)과 다르다.
 */
@Getter
@Entity
@Table(name = "access_roles")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessRole extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50, updatable = false)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 200)
	private String description;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "access_role_permissions", joinColumns = @JoinColumn(name = "access_role_id"))
	@Column(name = "permission", length = 100)
	private Set<String> permissions = new HashSet<>();

	public AccessRole(String code, String name, String description, Set<String> permissions) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.permissions = new HashSet<>(permissions);
	}

	public void modify(String name, String description, Set<String> permissions) {
		this.name = name;
		this.description = description;
		this.permissions.clear();
		this.permissions.addAll(permissions);
	}
}
