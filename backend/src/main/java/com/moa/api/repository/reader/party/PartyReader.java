package com.moa.api.repository.reader.party;

import static com.moa.api.entity.party.QParty.party;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.JoinRequestStatus;
import com.moa.api.dto.party.PartyStatus;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyJoinRequest;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyMessage;
import com.moa.api.entity.party.PartyPosition;
import com.moa.api.repository.jpa.party.PartyBanRepository;
import com.moa.api.repository.jpa.party.PartyJoinRequestRepository;
import com.moa.api.repository.jpa.party.PartyMemberRepository;
import com.moa.api.repository.jpa.party.PartyMessageRepository;
import com.moa.api.repository.jpa.party.PartyPositionRepository;
import com.moa.api.repository.jpa.party.PartyRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 파티 조회 (specs/008-party). 해산한 파티는 엔터티의 @SQLRestriction으로 빠진다.
 */
@Component
@RequiredArgsConstructor
public class PartyReader {

	private final JPAQueryFactory queryFactory;
	private final PartyRepository partyRepository;
	private final PartyPositionRepository positionRepository;
	private final PartyMemberRepository memberRepository;
	private final PartyJoinRequestRepository joinRequestRepository;
	private final PartyBanRepository banRepository;
	private final PartyMessageRepository messageRepository;

	public Optional<Party> find(Long id) {
		return partyRepository.findById(id);
	}

	/** 비관적 락 (research.md R6) */
	public Optional<Party> findForUpdate(Long id) {
		return partyRepository.findForUpdateById(id);
	}

	public Optional<Party> findByInviteCode(String inviteCode) {
		return partyRepository.findByInviteCode(inviteCode);
	}

	public boolean existsByInviteCode(String inviteCode) {
		return partyRepository.existsByInviteCode(inviteCode);
	}

	/**
	 * 모집 페이지 목록 (FR-004). 모집 중이고 공개·승인 방식인 것만 최신순이다.
	 *
	 * @param gameCode 없으면 모든 게임
	 * @param modeCode 없으면 모든 모드
	 */
	public Page<Party> findRecruitPage(String gameCode, String modeCode, Pageable pageable) {
		BooleanBuilder where = new BooleanBuilder(party.status.eq(PartyStatus.RECRUITING)
			.and(party.joinPolicy.in(JoinPolicy.OPEN, JoinPolicy.APPROVAL)));
		if (gameCode != null) {
			where.and(party.gameCode.eq(gameCode));
		}
		if (modeCode != null) {
			where.and(party.modeCode.eq(modeCode));
		}
		List<Party> content = queryFactory.selectFrom(party)
			.where(where)
			.orderBy(party.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory.select(party.count()).from(party).where(where).fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	public List<PartyPosition> findPositions(Long partyId) {
		return positionRepository.findByPartyIdOrderByIdAsc(partyId);
	}

	public List<PartyPosition> findPositions(List<Long> partyIds) {
		return partyIds.isEmpty() ? List.of() : positionRepository.findByPartyIdIn(partyIds);
	}

	/** 들어온 순서 (방장을 잇는 순서, FR-010) */
	public List<PartyMember> findMembers(Long partyId) {
		return memberRepository.findByPartyIdOrderByJoinedAtAscIdAsc(partyId);
	}

	public List<PartyMember> findMembers(List<Long> partyIds) {
		return partyIds.isEmpty() ? List.of() : memberRepository.findByPartyIdIn(partyIds);
	}

	/** 이 회원이 속한 파티의 멤버 행 (한 번에 한 파티, FR-026) */
	public Optional<PartyMember> findMembership(Long userId) {
		return memberRepository.findByUserId(userId);
	}

	public Optional<PartyMember> findMember(Long partyId, Long userId) {
		return memberRepository.findByPartyIdAndUserId(partyId, userId);
	}

	public boolean isMember(Long partyId, Long userId) {
		return memberRepository.existsByPartyIdAndUserId(partyId, userId);
	}

	public long countMembers(Long partyId) {
		return memberRepository.countByPartyId(partyId);
	}

	public boolean isBanned(Long partyId, Long userId) {
		return banRepository.existsByPartyIdAndUserId(partyId, userId);
	}

	/** 대기 중인 신청. 신청순 (US5-AC2) */
	public List<PartyJoinRequest> findPendingRequests(Long partyId) {
		return joinRequestRepository.findByPartyIdAndStatusOrderByIdAsc(partyId, JoinRequestStatus.PENDING);
	}

	/** 이 회원의 대기 중인 신청 (US8) */
	public List<PartyJoinRequest> findPendingRequestsOf(Long userId) {
		return joinRequestRepository.findByUserIdAndStatusOrderByIdAsc(userId, JoinRequestStatus.PENDING);
	}

	public Optional<PartyJoinRequest> findPendingRequest(Long partyId, Long userId) {
		return joinRequestRepository.findByPartyIdAndUserIdAndStatus(partyId, userId, JoinRequestStatus.PENDING);
	}

	public Optional<PartyJoinRequest> findRequest(Long id) {
		return joinRequestRepository.findById(id);
	}

	/**
	 * 마지막 메시지들 또는 before 앞의 메시지들. 오래된 것부터다 (research.md R5)
	 *
	 * @param limit 가져올 수. 부르는 쪽이 하나 더 달라고 해 다음이 있는지 본다
	 */
	public List<PartyMessage> findMessagesBefore(Long partyId, Long before, int limit) {
		PageRequest page = PageRequest.of(0, limit);
		List<PartyMessage> newestFirst = before == null
			? messageRepository.findByPartyIdOrderByIdDesc(partyId, page)
			: messageRepository.findByPartyIdAndIdLessThanOrderByIdDesc(partyId, before, page);
		return newestFirst.reversed();
	}

	/** after 뒤의 메시지들. 오래된 것부터다 (다시 연결된 뒤, FR-018) */
	public List<PartyMessage> findMessagesAfter(Long partyId, Long after, int limit) {
		return messageRepository.findByPartyIdAndIdGreaterThanOrderByIdAsc(partyId, after, PageRequest.of(0, limit));
	}
}
