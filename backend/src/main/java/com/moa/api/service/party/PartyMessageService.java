package com.moa.api.service.party;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyMessagePage;
import com.moa.api.entity.party.PartyMessage;
import com.moa.api.repository.reader.party.PartyReader;
import com.moa.api.repository.writer.party.PartyWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 파티 채팅 (specs/008-party US9). 지금 멤버만 읽고 보낸다 (FR-017). 이전 대화는 id 커서로 나눠 읽는다 (research.md R5).
 */
@Service
@RequiredArgsConstructor
public class PartyMessageService {

	private final PartyReader partyReader;
	private final PartyWriter partyWriter;

	/**
	 * 메시지 한 묶음. 오래된 것부터다. before도 after도 없으면 마지막 size개다 (US9-AC2, FR-018).
	 *
	 * @param before 이 id 앞의 것 (위로 올리기)
	 * @param after 이 id 뒤의 것 (다시 연결된 뒤)
	 * @throws ServiceException 해산했거나 없는 파티 R001, 멤버가 아니면 A102, before와 after를 함께 주면 V001
	 */
	public PartyMessagePage getPage(Long partyId, Long userId, Long before, Long after, int size) {
		requireMember(partyId, userId);
		if (before != null && after != null) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
				List.of(new FieldError("after", "before와 after는 함께 쓸 수 없습니다.")));
		}
		if (after != null) {
			List<PartyMessage> found = partyReader.findMessagesAfter(partyId, after, size + 1);
			boolean hasMore = found.size() > size;
			return new PartyMessagePage(dtos(hasMore ? found.subList(0, size) : found), hasMore);
		}
		List<PartyMessage> found = partyReader.findMessagesBefore(partyId, before, size + 1);
		boolean hasMore = found.size() > size;
		// 오래된 것부터라 하나 더 가져온 것은 맨 앞에 있다
		return new PartyMessagePage(dtos(hasMore ? found.subList(1, found.size()) : found), hasMore);
	}

	/**
	 * 메시지를 보낸다 (US9-AC1). 글자 수는 요청 검증이 본다 (US9-AC5).
	 *
	 * @throws ServiceException 해산했거나 없는 파티 R001, 멤버가 아니면 A102
	 */
	public PartyMessageDto send(Long partyId, Long userId, String content) {
		requireMember(partyId, userId);
		return PartyMessageDto.from(partyWriter.saveMessage(PartyMessage.chat(partyId, userId, content)));
	}

	private void requireMember(Long partyId, Long userId) {
		partyReader.find(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (!partyReader.isMember(partyId, userId)) {
			throw new ServiceException(ErrorCode.PARTY_MEMBER_ONLY);
		}
	}

	private static List<PartyMessageDto> dtos(List<PartyMessage> messages) {
		return messages.stream().map(PartyMessageDto::from).toList();
	}
}
