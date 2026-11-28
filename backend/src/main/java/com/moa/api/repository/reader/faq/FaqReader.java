package com.moa.api.repository.reader.faq;

import static com.moa.api.entity.faq.QFaq.faq;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.moa.api.entity.faq.Faq;
import com.moa.api.repository.jpa.faq.FaqRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * FAQ 조회 (specs/017-customer-center). 지운 FAQ는 엔터티의 @SQLRestriction으로 빠진다.
 * 상한(200개)이 있는 구성 목록이라 쪽을 나누지 않는다 (FR-015).
 */
@Component
@RequiredArgsConstructor
public class FaqReader {

	private static final OrderSpecifier<?>[] ORDER = {faq.sortOrder.asc(), faq.createdAt.desc(), faq.id.desc()};

	private final JPAQueryFactory queryFactory;
	private final FaqRepository faqRepository;

	public Optional<Faq> findById(Long id) {
		return faqRepository.findById(id);
	}

	public List<Faq> findAllByIdIn(Collection<Long> ids) {
		return faqRepository.findAllByIdIn(ids);
	}

	/** 지우지 않은 FAQ 수 (상한 확인) */
	public long count() {
		return faqRepository.count();
	}

	/**
	 * 관리 목록. 순서가 작은 것, 같으면 나중에 등록한 것부터 (FR-006).
	 *
	 * @param keyword 질문에 들어 있는 글자
	 */
	public List<Faq> findAdminList(@Nullable String categoryCode, @Nullable Boolean enabled,
		@Nullable String keyword) {
		BooleanBuilder where = new BooleanBuilder();
		if (categoryCode != null && !categoryCode.isBlank()) {
			where.and(faq.categoryCode.eq(categoryCode));
		}
		if (enabled != null) {
			where.and(faq.enabled.eq(enabled));
		}
		if (keyword != null && !keyword.isBlank()) {
			where.and(faq.question.contains(keyword.trim()));
		}
		return queryFactory.selectFrom(faq).where(where).orderBy(ORDER).fetch();
	}

	/**
	 * 사용자 목록. 켜져 있고 분류가 지금 고를 수 있는 항목인 FAQ만 (FR-013, research R9).
	 *
	 * @param activeCategories 사용 중인 분류 코드
	 * @param keyword 질문이나 답에 들어 있는 글자
	 */
	public List<Faq> findPublicList(Collection<String> activeCategories, @Nullable String categoryCode,
		@Nullable String keyword) {
		if (activeCategories.isEmpty()) {
			return List.of();
		}
		BooleanBuilder where = new BooleanBuilder(faq.enabled.isTrue()).and(faq.categoryCode.in(activeCategories));
		if (categoryCode != null && !categoryCode.isBlank()) {
			where.and(faq.categoryCode.eq(categoryCode));
		}
		if (keyword != null && !keyword.isBlank()) {
			String trimmed = keyword.trim();
			where.and(faq.question.contains(trimmed).or(faq.answer.contains(trimmed)));
		}
		return queryFactory.selectFrom(faq).where(where).orderBy(ORDER).fetch();
	}
}
