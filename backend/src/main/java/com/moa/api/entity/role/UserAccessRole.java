package com.moa.api.entity.role;

import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

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
 * 사용자에게 준 역할 (선택 헬퍼 rbac).
 */
@Getter
@Entity
@Table(name = "user_access_roles")
@HardDelete(reason = "역할을 빼면 지운다. 누가 바꿨는지는 역할을 다시 줄 때의 감사 열에 남는다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccessRole extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "access_role_id", nullable = false)
	private Long accessRoleId;

	public UserAccessRole(Long userId, Long accessRoleId) {
		this.userId = userId;
		this.accessRoleId = accessRoleId;
	}
}
