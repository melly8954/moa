package com.moa.api.repository.jpa.party;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.party.PartyMessage;

/**
 * 파티 메시지. 이전 대화는 id 커서로 나눠 읽는다 (specs/008-party research R5).
 * 부르는 쪽이 한 개 더(51개) 가져와 다음이 있는지(hasMore) 본다.
 */
public interface PartyMessageRepository extends JpaRepository<PartyMessage, Long> {

	/** 마지막 메시지들. 큰 id부터라 부르는 쪽이 뒤집는다 */
	List<PartyMessage> findByPartyIdOrderByIdDesc(Long partyId, Pageable pageable);

	/** before보다 앞의 메시지들 (위로 올리기). 큰 id부터라 부르는 쪽이 뒤집는다 */
	List<PartyMessage> findByPartyIdAndIdLessThanOrderByIdDesc(Long partyId, Long before, Pageable pageable);

	/** after보다 뒤의 메시지들 (다시 연결된 뒤). 작은 id부터다 */
	List<PartyMessage> findByPartyIdAndIdGreaterThanOrderByIdAsc(Long partyId, Long after, Pageable pageable);
}
