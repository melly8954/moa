package com.moa.api.controller.v1.party;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.party.PartyMessageApplication;
import com.moa.api.controller.v1.party.request.PartyMessageCreateRequest;
import com.moa.api.controller.v1.party.response.PartyMessagePageResponse;
import com.moa.api.controller.v1.party.response.PartyMessageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 파티 채팅 (specs/008-party US9). 지금 멤버만 읽고 보낸다. 받기는 STOMP /topic/parties/{id}다 (contracts/party-realtime.md).
 */
@Tag(name = "파티 채팅 API")
@RestController
@RequestMapping("/api/v1/parties/{id}/messages")
@RequiredArgsConstructor
public class ApiPartyMessageController {

	private final PartyMessageApplication partyMessageApplication;

	@Operation(summary = "메시지 목록",
		description = "오래된 것부터. before가 있으면 그 앞, after가 있으면 그 뒤(다시 연결된 뒤), 둘 다 없으면 마지막 size개. 멤버가 아니면 A102, 해산했으면 R001")
	@GetMapping
	public ResponseEntity<PartyMessagePageResponse> getPage(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@RequestParam(required = false) Long before,
		@RequestParam(required = false) Long after,
		@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PartyMessagePageResponse.from(
			partyMessageApplication.getPage(user.userId(), id, before, after, size)));
	}

	@Operation(summary = "메시지 보내기", description = "1~500자. 커밋 뒤 파티 주소로 보낸다. 멤버가 아니면 A102, 해산했으면 R001")
	@PostMapping
	public ResponseEntity<PartyMessageResponse> send(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@Valid @RequestBody PartyMessageCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(PartyMessageResponse.from(
			partyMessageApplication.send(user.userId(), id, request.content())));
	}
}
