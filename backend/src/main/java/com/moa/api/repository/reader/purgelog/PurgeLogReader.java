package com.moa.api.repository.reader.purgelog;

import static com.moa.common.purgelog.QPurgeLog.purgeLog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.common.purgelog.PurgeLog;
import com.moa.common.purgelog.PurgeTarget;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 파기 이력 조회 (specs/013-purge-log FR-009). 조건은 모두 선택이다.
 * 인덱스 created_at, target+created_at을 탄다.
 */
@Component
@RequiredArgsConstructor
public class PurgeLogReader {

	private final JPAQueryFactory queryFactory;

	/** 최신순 (created_at desc, id desc) */
	public Page<PurgeLog> findPage(PurgeTarget target, LocalDate from, LocalDate to, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		if (target != null) {
			where.and(purgeLog.target.eq(target));
		}
		if (from != null) {
			where.and(purgeLog.createdAt.goe(from.atStartOfDay()));
		}
		if (to != null) {
			where.and(purgeLog.createdAt.lt(to.plusDays(1).atStartOfDay()));
		}
		List<PurgeLog> content = queryFactory.selectFrom(purgeLog)
			.where(where)
			.orderBy(purgeLog.createdAt.desc(), purgeLog.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(purgeLog.count()).from(purgeLog).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
