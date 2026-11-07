package com.moa.api.repository.reader.loginhistory;

import static com.moa.common.helper.loginhistory.QLoginHistory.loginHistory;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.common.helper.loginhistory.LoginHistory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 접속 이력 조회. 조건은 모두 선택이다. 기존 인덱스를 탄다
 * (login_id+created_at, user_id+created_at, created_at).
 */
@Component
@RequiredArgsConstructor
public class LoginHistoryReader {

	private final JPAQueryFactory queryFactory;

	/** 최신순 (created_at desc, id desc) */
	public Page<LoginHistory> findPage(Long userId, String loginId, Boolean success, LocalDate from, LocalDate to,
		Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		if (userId != null) {
			where.and(loginHistory.userId.eq(userId));
		}
		if (loginId != null && !loginId.isBlank()) {
			where.and(loginHistory.loginId.eq(loginId));
		}
		if (success != null) {
			where.and(loginHistory.success.eq(success));
		}
		if (from != null) {
			where.and(loginHistory.createdAt.goe(from.atStartOfDay()));
		}
		if (to != null) {
			where.and(loginHistory.createdAt.lt(to.plusDays(1).atStartOfDay()));
		}
		List<LoginHistory> content = queryFactory.selectFrom(loginHistory)
			.where(where)
			.orderBy(loginHistory.createdAt.desc(), loginHistory.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(loginHistory.count()).from(loginHistory).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
