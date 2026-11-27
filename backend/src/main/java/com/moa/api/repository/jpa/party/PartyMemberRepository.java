package com.moa.api.repository.jpa.party;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
