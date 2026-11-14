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
import com.moa.api.repository.jpa.notice.NoticeReadRepository;
import com.moa.api.repository.jpa.notice.NoticeRepository;
import com.querydsl.core.BooleanBuilder;
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

	/** 사용자 목록. 게시한 공지만, 고정 먼저, 그다음 게시 최신순 (FR-005) */
	public Page<Notice> findPublishedPage(Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder(notice.status.eq(NoticeStatus.PUBLISHED));
		List<Notice> content = queryFactory.selectFrom(notice)
			.where(where)
			.orderBy(notice.pinned.desc(), notice.publishedAt.desc(), notice.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(notice.count()).from(notice).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
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
