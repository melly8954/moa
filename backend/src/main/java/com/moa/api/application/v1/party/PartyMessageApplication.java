package com.moa.api.application.v1.party;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.party.MemberRef;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyMessagePage;
import com.moa.api.dto.party.PartyMessagePageView;
import com.moa.api.dto.party.PartyMessageView;
import com.moa.api.service.party.PartyMessageService;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 파티 채팅 (specs/008-party US9). 보내기는 REST로 하고, 커밋 뒤 파티 주소로 MESSAGE를 보낸다 (research.md R2).
 */
@Component
@RequiredArgsConstructor
public class PartyMessageApplication {

	private final PartyMessageService partyMessageService;
	private final PartyApplication partyApplication;
	private final UserService userService;

	/** 메시지 한 묶음. 오래된 것부터 (US9-AC2, FR-018) */
	@Transactional(readOnly = true)
	public PartyMessagePageView getPage(Long userId, Long partyId, Long before, Long after, int size) {
		PartyMessagePage page = partyMessageService.getPage(partyId, userId, before, after, size);
		Map<Long, String> names = userService.displayNames(page.items().stream()
			.flatMap(message -> PartyPayloads.userIdsOf(message).stream())
			.distinct()
			.toList());
		return new PartyMessagePageView(page.items().stream().map(message -> view(message, names)).toList(),
			page.hasMore());
	}

	/** 메시지를 보낸다 (US9-AC1) */
	@Transactional
	public PartyMessageView send(Long userId, Long partyId, String content) {
		PartyMessageDto message = partyMessageService.send(partyId, userId, content);
		partyApplication.publishMessage(message);
		return view(message, userService.displayNames(List.of(userId)));
	}

	private static PartyMessageView view(PartyMessageDto message, Map<Long, String> names) {
		return new PartyMessageView(message, ref(message.senderUserId(), names), ref(message.subjectUserId(), names));
	}

	private static MemberRef ref(Long userId, Map<Long, String> names) {
		return userId == null ? null : new MemberRef(userId, names.get(userId));
	}
}
