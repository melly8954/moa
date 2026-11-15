package com.moa.api.dto.role;

import com.moa.common.security.Role;

/**
 * 역할 드로어 "구성원" 탭의 한 줄 (moa, specs/003-admin-roles FR-006).
 *
 * @param role 시스템 역할. ADMIN은 업무 역할을 받지 않는다
 * @param member 이 역할의 구성원인가
 */
public record RoleMemberDto(Long id, String loginId, String name, Role role, boolean member) {
}
