package com.moa.api.repository.jpa.party;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.dto.party.JoinRequestStatus;
import com.moa.api.entity.party.PartyJoinRequest;

public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {

	/** 신청순 (US5-AC2) */
	List<PartyJoinRequest> findByPartyIdAndStatusOrderByIdAsc(Long partyId, JoinRequestStatus status);

	List<PartyJoinRequest> findByUserIdAndStatusOrderByIdAsc(Long userId, JoinRequestStatus status);

	Optional<PartyJoinRequest> findByPartyIdAndUserIdAndStatus(Long partyId, Long userId, JoinRequestStatus status);

	void deleteByPartyId(Long partyId);
}
