package com.moa.api.repository.jpa.party;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.party.PartyPosition;

public interface PartyPositionRepository extends JpaRepository<PartyPosition, Long> {

	List<PartyPosition> findByPartyIdOrderByIdAsc(Long partyId);

	List<PartyPosition> findByPartyIdIn(List<Long> partyIds);

	void deleteByPartyId(Long partyId);
}
