package com.moa.api.repository.writer.party;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyBan;
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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PartyWriter {

	private final PartyRepository partyRepository;
	private final PartyPositionRepository positionRepository;
	private final PartyMemberRepository memberRepository;
	private final PartyJoinRequestRepository joinRequestRepository;
	private final PartyBanRepository banRepository;
	private final PartyMessageRepository messageRepository;

	public Party save(Party party) {
		return partyRepository.save(party);
	}

	public List<PartyPosition> savePositions(List<PartyPosition> positions) {
		return positionRepository.saveAll(positions);
	}

	public void deletePositions(List<PartyPosition> positions) {
		positionRepository.deleteAll(positions);
	}

	/**
	 * 멤버를 바로 DB에 넣는다. 한 번에 한 파티(uk_party_member_user_id)를 어기면 그 자리에서
	 * DataIntegrityViolationException이 난다 (research.md R6)
	 */
	public PartyMember saveMember(PartyMember member) {
		return memberRepository.saveAndFlush(member);
	}

	public void deleteMember(PartyMember member) {
		memberRepository.delete(member);
		memberRepository.flush();
	}

	public PartyJoinRequest saveRequest(PartyJoinRequest request) {
		return joinRequestRepository.saveAndFlush(request);
	}

	public PartyBan saveBan(PartyBan ban) {
		return banRepository.save(ban);
	}

	public PartyMessage saveMessage(PartyMessage message) {
		return messageRepository.save(message);
	}

	/** 해산할 때 멤버·포지션 자리·신청을 지운다. 메시지와 참가 제한은 남긴다 (research.md R12) */
	public void deleteDissolved(Long partyId) {
		memberRepository.deleteByPartyId(partyId);
		positionRepository.deleteByPartyId(partyId);
		joinRequestRepository.deleteByPartyId(partyId);
	}
}
