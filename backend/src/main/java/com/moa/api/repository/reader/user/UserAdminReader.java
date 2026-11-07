package com.moa.api.repository.reader.user;

import static com.moa.api.entity.user.QUser.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.api.repository.jpa.user.UserRepository;
import com.moa.common.security.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 조회 (선택 헬퍼 user-admin). 지운 사용자와 시스템 계정은 빠진다.
 */
@Component
@RequiredArgsConstructor
public class UserAdminReader {

	private final JPAQueryFactory queryFactory;
	private final UserRepository userRepository;

	/**
	 * 최신순 (created_at desc, id desc). 조건은 모두 선택이다.
	 *
	 * @param keyword 아이디나 이름에 들어 있는 글자
	 */
	public Page<User> findPage(Long systemActorId, String keyword, Role role, Boolean locked, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder(user.id.ne(systemActorId));
		if (keyword != null && !keyword.isBlank()) {
			String trimmed = keyword.trim();
			where.and(user.loginId.contains(trimmed).or(user.name.contains(trimmed)));
		}
		if (role != null) {
			where.and(user.role.eq(role));
		}
		if (locked != null) {
			where.and(locked ? user.lockedAt.isNotNull() : user.lockedAt.isNull());
		}
		List<User> content = queryFactory.selectFrom(user)
			.where(where)
			.orderBy(user.createdAt.desc(), user.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(user.count()).from(user).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	public boolean existsLoginId(String loginId) {
		return userRepository.findByLoginId(loginId).isPresent();
	}
}
