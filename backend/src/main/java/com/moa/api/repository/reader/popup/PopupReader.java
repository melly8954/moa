package com.moa.api.repository.reader.popup;

import static com.moa.api.entity.popup.QPopup.popup;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.moa.api.entity.popup.Popup;
import com.moa.api.repository.jpa.popup.PopupRepository;
import com.moa.api.repository.reader.marketing.ExposurePredicates;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 팝업 조회 (specs/016-banner-popup). 지운 팝업은 엔터티의 @SQLRestriction으로 빠진다.
 */
@Component
@RequiredArgsConstructor
public class PopupReader {

	private final JPAQueryFactory queryFactory;
	private final PopupRepository popupRepository;

	public Optional<Popup> findById(Long id) {
		return popupRepository.findById(id);
	}

	public List<Popup> findAllByIdIn(Collection<Long> ids) {
		return popupRepository.findAllByIdIn(ids);
	}

	/**
	 * 관리 목록. 순서가 작은 것, 같으면 노출 시작이 늦은 것부터 (FR-005).
	 *
	 * @param keyword 제목에 들어 있는 글자
	 * @param status 비면 끝난 것만 뺀다 (research R4)
	 */
	public Page<Popup> findAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		LocalDateTime now, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		if (keyword != null && !keyword.isBlank()) {
			where.and(popup.title.contains(keyword.trim()));
		}
		where.and(ExposurePredicates.of(status, popup.enabled, popup.startsAt, popup.endsAt, now));
		List<Popup> content = queryFactory.selectFrom(popup)
			.where(where)
			.orderBy(popup.sortOrder.asc(), popup.startsAt.desc(), popup.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(popup.count()).from(popup).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
