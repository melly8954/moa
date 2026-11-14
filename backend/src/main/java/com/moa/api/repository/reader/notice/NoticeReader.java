package com.moa.api.repository.reader.notice;

import static com.moa.api.entity.notice.QNotice.notice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.dto.notice.NoticeStatus;
import com.moa.api.entity.notice.Notice;
import com.moa.api.entity.notice.NoticeRead;
import com.moa.api.entity.notice.QNotice;
import com.moa.api.repository.jpa.notice.NoticeReadRepository;
import com.moa.api.repository.jpa.notice.NoticeRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 공지 조회 (specs/006-notices). 지운 공지는 엔터티의 @SQLRestriction으로 빠진다.
 */
@Component
@RequiredArgsConstructor
public class NoticeReader {

	private final JPAQueryFactory queryFactory;
	private final NoticeRepository noticeRepository;
	private final NoticeReadRepository noticeReadRepository;

	public Optional<Notice> findById(Long id) {
		return noticeRepository.findById(id);
	}

	/**
	 * 관리 목록. 고친 최신순 (updated_at desc, id desc). 조건은 모두 선택이다.
	 *
	 * @param keyword 제목에 들어 있는 글자
	 */
	public Page<Notice> findAdminPage(String keyword, NoticeStatus status, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		if (keyword != null && !keyword.isBlank()) {
			where.and(notice.title.contains(keyword.trim()));
		}
		if (status != null) {
			where.and(notice.status.eq(status));
		}
		List<Notice> content = queryFactory.selectFrom(notice)
			.where(where)
			.orderBy(notice.updatedAt.desc(), notice.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(notice.count()).from(notice).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/**
	 * 사용자 목록. 게시한 공지만, 고정 먼저, 그다음 게시 최신순 (FR-005).
	 * 행마다 게시 순번을 함께 센다: 지우지 않은 게시 공지 중 이 공지보다 먼저(같은 시각이면 id가 작거나 같게) 게시한 것의 수
	 * (FR-010, research R11).
	 */
	public Page<NumberedNotice> findPublishedPage(Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder(notice.status.eq(NoticeStatus.PUBLISHED));
		QNotice earlier = new QNotice("earlier");
		List<Tuple> rows = queryFactory.select(notice, JPAExpressions.select(earlier.count())
			.from(earlier)
			.where(earlier.status.eq(NoticeStatus.PUBLISHED), earlier.deletedAt.isNull(),
				earlier.publishedAt.lt(notice.publishedAt)
					.or(earlier.publishedAt.eq(notice.publishedAt).and(earlier.id.loe(notice.id)))))
			.from(notice)
			.where(where)
			.orderBy(notice.pinned.desc(), notice.publishedAt.desc(), notice.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		List<NumberedNotice> content = rows.stream()
			.map(row -> new NumberedNotice(row.get(0, Notice.class), row.get(1, Long.class)))
			.toList();
		Long total = queryFactory.select(notice.count()).from(notice).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/** 게시 순번을 붙인 공지 */
	public record NumberedNotice(Notice notice, long number) {
	}

	/** 이 시각 뒤에 게시된 공지가 있나. 시각이 없으면 게시한 공지가 하나라도 있나 */
	public boolean existsPublishedAfter(LocalDateTime since) {
		BooleanBuilder where = new BooleanBuilder(notice.status.eq(NoticeStatus.PUBLISHED));
		if (since != null) {
			where.and(notice.publishedAt.after(since));
		}
		return queryFactory.selectOne().from(notice).where(where).fetchFirst() != null;
	}

	public Optional<NoticeRead> findRead(Long userId) {
		return noticeReadRepository.findById(userId);
	}
}
