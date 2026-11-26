package com.moa.api.repository.reader.inquiry;

import static com.moa.api.entity.inquiry.QInquiry.inquiry;
import static com.moa.api.entity.user.QUser.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.dto.inquiry.InquiryAuthor;
import com.moa.api.dto.inquiry.InquiryStatusFilter;
import com.moa.api.entity.inquiry.Inquiry;
import com.moa.api.repository.jpa.inquiry.InquiryRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 1:1 문의 조회 (specs/017-customer-center). 작성자 아이디·이름은 회원 행을 함께 읽는다 (RoleUserReader와 같은 방식).
 */
@Component
@RequiredArgsConstructor
public class InquiryReader {

	private final JPAQueryFactory queryFactory;
	private final InquiryRepository inquiryRepository;

	public Optional<Inquiry> findById(Long id) {
		return inquiryRepository.findById(id);
	}

	/** 답변 저장용 잠금 읽기 (research R3) */
	public Optional<Inquiry> findByIdForUpdate(Long id) {
		return inquiryRepository.findByIdForUpdate(id);
	}

	/** 내 문의. 쓴 최신순 (FR-013) */
	public Page<Inquiry> findMyPage(Long userId, Pageable pageable) {
		List<Inquiry> content = queryFactory.selectFrom(inquiry)
			.where(inquiry.userId.eq(userId))
			.orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(inquiry.count()).from(inquiry).where(inquiry.userId.eq(userId)).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/**
	 * 관리 목록. 쓴 최신순 (research R6).
	 *
	 * @param status 비면 답변 대기
	 * @param from 쓴 날짜 시작 (포함)
	 * @param to 쓴 날짜 끝 (포함, 그날 끝까지)
	 * @param keyword 제목, 작성자 아이디·이름에 들어 있는 글자
	 */
	public Page<InquiryWithAuthor> findAdminPage(@Nullable InquiryStatusFilter status, @Nullable String categoryCode,
		@Nullable LocalDate from, @Nullable LocalDate to, @Nullable String keyword, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder();
		InquiryStatusFilter filter = status == null ? InquiryStatusFilter.WAITING : status;
		if (filter == InquiryStatusFilter.WAITING) {
			where.and(inquiry.answeredAt.isNull());
		} else if (filter == InquiryStatusFilter.ANSWERED) {
			where.and(inquiry.answeredAt.isNotNull());
		}
		if (categoryCode != null && !categoryCode.isBlank()) {
			where.and(inquiry.categoryCode.eq(categoryCode));
		}
		if (from != null) {
			where.and(inquiry.createdAt.goe(from.atStartOfDay()));
		}
		if (to != null) {
			where.and(inquiry.createdAt.lt(to.plusDays(1).atStartOfDay()));
		}
		if (keyword != null && !keyword.isBlank()) {
			String trimmed = keyword.trim();
			where.and(inquiry.title.contains(trimmed)
				.or(user.loginId.contains(trimmed))
				.or(user.name.contains(trimmed)));
		}
		List<InquiryWithAuthor> content = queryFactory.select(inquiry, user.loginId, user.name)
			.from(inquiry)
			.leftJoin(user).on(user.id.eq(inquiry.userId))
			.where(where)
			.orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch()
			.stream()
			.map(InquiryReader::toRow)
			.toList();
		Long total = queryFactory.select(inquiry.count())
			.from(inquiry)
			.leftJoin(user).on(user.id.eq(inquiry.userId))
			.where(where)
			.fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	/** 쓴 회원 (관리 상세) */
	public InquiryAuthor findAuthor(Long userId) {
		Tuple row = queryFactory.select(user.loginId, user.name).from(user).where(user.id.eq(userId)).fetchOne();
		return row == null
			? new InquiryAuthor(userId, null, null)
			: new InquiryAuthor(userId, row.get(user.loginId), row.get(user.name));
	}

	/** 답변한 관리자 이름 (관리 상세). 없으면 빈 값 */
	public Optional<String> findUserName(Long userId) {
		return Optional.ofNullable(queryFactory.select(user.name).from(user).where(user.id.eq(userId)).fetchOne());
	}

	private static InquiryWithAuthor toRow(Tuple row) {
		Inquiry found = row.get(inquiry);
		return new InquiryWithAuthor(found,
			new InquiryAuthor(found.getUserId(), row.get(user.loginId), row.get(user.name)));
	}
}
