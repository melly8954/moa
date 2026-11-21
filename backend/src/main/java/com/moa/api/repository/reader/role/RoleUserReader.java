package com.moa.api.repository.reader.role;

import static com.moa.api.entity.role.QUserAccessRole.userAccessRole;
import static com.moa.api.entity.user.QUser.user;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.common.security.Role;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 권한 관리에서 관리자 계정 찾기 (moa, specs/003-admin-roles FR-006). 지운 사용자와 시스템 계정은 빠진다.
 * 사용자 관리(user-admin) 권한 없이 권한 관리 권한(ROLE_MANAGE)으로 쓴다.
 */
@Component
@RequiredArgsConstructor
public class RoleUserReader {

	private final JPAQueryFactory queryFactory;

	/** 이 역할의 구성원, 아이디 순 쪽 */
	public Page<User> findMembersPage(Long systemActorId, Long roleId, Pageable pageable) {
		return page(user.id.ne(systemActorId).and(user.id.in(JPAExpressions.select(userAccessRole.userId)
			.from(userAccessRole)
			.where(userAccessRole.accessRoleId.eq(roleId)))), pageable);
	}

	/**
	 * 아이디·이름에 이 글자가 든 관리자 계정, 아이디 순 쪽. moa: 회원(USER)은 역할을 받지 않으므로 찾지 않는다
	 * (specs/003-admin-roles FR-006, 2026-11-21)
	 */
	public Page<User> findSearchPage(Long systemActorId, String keyword, Pageable pageable) {
		String trimmed = keyword.trim();
		return page(user.id.ne(systemActorId)
			.and(user.role.eq(Role.ADMIN))
			.and(user.loginId.contains(trimmed).or(user.name.contains(trimmed))), pageable);
	}

	/** 이 ID의 사용자들 (지운 사용자·시스템 계정은 빠진다) */
	public List<User> findAll(Long systemActorId, Collection<Long> ids) {
		return queryFactory.selectFrom(user).where(user.id.in(ids).and(user.id.ne(systemActorId))).fetch();
	}

	private Page<User> page(BooleanExpression where, Pageable pageable) {
		List<User> content = queryFactory.selectFrom(user)
			.where(where)
			.orderBy(user.loginId.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(user.count()).from(user).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
