package com.moa.api.repository.jpa.party;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.party.PartyBan;

public interface PartyBanRepository extends JpaRepository<PartyBan, Long> {

	boolean existsByPartyIdAndUserId(Long partyId, Long userId);
}
