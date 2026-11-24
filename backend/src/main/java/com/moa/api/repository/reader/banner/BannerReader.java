package com.moa.api.repository.reader.banner;

import static com.moa.api.entity.banner.QBanner.banner;

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
import com.moa.api.entity.banner.Banner;
import com.moa.api.repository.jpa.banner.BannerRepository;
import com.moa.api.repository.reader.marketing.ExposurePredicates;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 배너 조회 (specs/016-banner-popup). 지운 배너는 엔터티의 @SQLRestriction으로 빠진다.
 */
@Component
@RequiredArgsConstructor
public class BannerReader {

	private final JPAQueryFactory queryFactory;
	private final BannerRepository bannerRepository;

	public Optional<Banner> findById(Long id) {
		return bannerRepository.findById(id);
	}

	public List<Banner> findAllByIdIn(Collection<Long> ids) {
		return bannerRepository.findAllByIdIn(ids);
	}

	/**
	 * 관리 목록. 순서가 작은 것, 같으면 노출 시작이 늦은 것부터 (FR-005).
	 *
	 * @param keyword 이름에 들어 있는 글자
	 * @param status 비면 끝난 것만 뺀다 (research R4)
	 */
	public Page<Banner> findAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		LocalDateTime now, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		if (keyword != null && !keyword.isBlank()) {
			where.and(banner.name.contains(keyword.trim()));
		}
		where.and(ExposurePredicates.of(status, banner.enabled, banner.startsAt, banner.endsAt, now));
		List<Banner> content = queryFactory.selectFrom(banner)
			.where(where)
			.orderBy(banner.sortOrder.asc(), banner.startsAt.desc(), banner.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(banner.count()).from(banner).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
