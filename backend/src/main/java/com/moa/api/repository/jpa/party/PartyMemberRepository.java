package com.moa.api.repository.jpa.party;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.party.PartyMember;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

	/** 들어온 순서. 같은 시각이면 id가 작은 쪽이 먼저다 (FR-010) */
	List<PartyMember> findByPartyIdOrderByJoinedAtAscIdAsc(Long partyId);

	List<PartyMember> findByPartyIdIn(List<Long> partyIds);

	Optional<PartyMember> findByUserId(Long userId);

	Optional<PartyMember> findByPartyIdAndUserId(Long partyId, Long userId);

	boolean existsByPartyIdAndUserId(Long partyId, Long userId);

	long countByPartyId(Long partyId);

	void deleteByPartyId(Long partyId);

	/**
	 * 탈퇴했는데 멤버 행이나 대기 신청이 남은 회원 (specs/008-party FR-031, research R15). 탈퇴한 회원은 회원 엔터티의 기본 조회에서
	 * 빠지므로 네이티브 쿼리로 찾는다
	 */
	@Query(value = "select u.id from tb_user u where u.withdrawn_at is not null"
		+ " and (exists (select 1 from tb_party_member m where m.user_id = u.id)"
		+ " or exists (select 1 from tb_party_join_request r where r.user_id = u.id and r.status = 'PENDING'))"
		+ " order by u.id", nativeQuery = true)
	List<Long> findWithdrawnUserIdsWithLeftovers();
}
