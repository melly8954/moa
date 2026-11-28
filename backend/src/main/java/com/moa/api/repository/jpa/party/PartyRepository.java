package com.moa.api.repository.jpa.party;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.party.Party;
import com.moa.common.repository.SoftDeleteRepository;

import jakarta.persistence.LockModeType;

public interface PartyRepository extends SoftDeleteRepository<Party, Long> {

	/** 파티 행을 비관적 락으로 잡는다. 멤버가 되거나 빠지는 모든 쓰기가 이 락 안에서 센다 (specs/008-party research R6) */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Party p where p.id = :id")
	Optional<Party> findForUpdateById(@Param("id") Long id);

	Optional<Party> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

	List<Party> findByIdIn(Collection<Long> ids);
}
