package com.moa.api.repository.reader.role;

import static com.moa.api.entity.role.QUserAccessRole.userAccessRole;
import static com.moa.api.entity.user.QUser.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.common.security.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 역할 관리에서 사용자 찾기 (moa, specs/003-admin-roles FR-005·FR-006). 지운 사용자와 시스템 계정은 빠진다.
 * 사용자 관리(user-admin) 권한 없이 역할 관리 권한으로 쓴다.
 */
@Component
@RequiredArgsConstructor
public class RoleUserReader {

	private final JPAQueryFactory queryFactory;

	/**
	 * 아이디 순 페이지. keyword가 없으면 업무 역할이 하나라도 있는 사용자와 시스템 역할 ADMIN인 사용자,
	 * 있으면 아이디·이름에 그 글자가 든 모든 사용자다.
	 */
	public Page<User> findPage(Long systemActorId, String keyword, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder(user.id.ne(systemActorId));
		if (keyword != null && !keyword.isBlank()) {
			String trimmed = keyword.trim();
			where.and(user.loginId.contains(trimmed).or(user.name.contains(trimmed)));
		} else {
			where.and(user.role.eq(Role.ADMIN)
				.or(user.id.in(JPAExpressions.select(userAccessRole.userId).from(userAccessRole))));
		}
		List<User> content = queryFactory.selectFrom(user)
			.where(where)
			.orderBy(user.loginId.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(user.count()).from(user).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/** 이 역할을 가진 사용자, 아이디 순 */
	public List<User> findMembers(Long roleId) {
		return queryFactory.selectFrom(user)
			.where(user.id.in(JPAExpressions.select(userAccessRole.userId)
				.from(userAccessRole)
				.where(userAccessRole.accessRoleId.eq(roleId))))
			.orderBy(user.loginId.asc())
			.fetch();
	}
}
