package com.moa.api.dto.role;

/**
 * 역할 드로어 "구성원" 탭의 한 줄 (moa, specs/003-admin-roles FR-006).
 *
 * @param superAdmin moa: 최고 관리자 역할을 가졌나 (specs/003-admin-roles FR-006, 2026-11-21). 다른 역할을 받지 않는다
 * @param member 이 역할의 구성원인가
 */
public record RoleMemberDto(Long id, String loginId, String name, boolean superAdmin, boolean member) {
}
