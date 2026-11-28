package com.moa.api.controller.v1.party;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.party.PartyApplication;
import com.moa.api.controller.v1.party.request.PartyMemberCreateRequest;
import com.moa.api.controller.v1.party.response.PartyDetailResponse;
import com.moa.api.controller.v1.party.response.PartyInvitePreviewResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 파티 참가·초대 입장·나가기·내보내기 (specs/008-party US3, US4, US6, US7). 멤버가 되는 세 길은 같은 순서로 확인한다
 * (contracts/party-api.md "멤버가 되는 모든 길"). 조건이 없는 파티면 본문 없이 불러도 된다.
 */
@Tag(name = "파티 멤버 API")
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class ApiPartyMemberController {

	private final PartyApplication partyApplication;

	@Operation(summary = "공개 파티 참가",
		description = "B101 정원, B102 마감, B103 이미 속함, B104 참가 제한, B105 포지션 참, B106 포지션 필요, B112 공개가 아님, A103 관리자 계정")
	@PostMapping("/{id}/members")
	public ResponseEntity<PartyDetailResponse> join(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@Valid @RequestBody(required = false) PartyMemberCreateRequest request) {
		PartyMemberCreateRequest body = orEmpty(request);
		return ResponseEntity.ok(PartyDetailResponse.from(
			partyApplication.join(user.userId(), id, body.positionCode(), body.tierCode())));
	}

	@Operation(summary = "초대 미리보기", description = "로그인 없이 연다. 없는 코드, 바뀐 코드, 해산한 파티면 B107")
	@PermitAll
	@SecurityRequirements
	@GetMapping("/invites/{code}")
	public ResponseEntity<PartyInvitePreviewResponse> getInvite(@CurrentUser AuthPrincipal user,
		@PathVariable String code) {
		return ResponseEntity.ok(PartyInvitePreviewResponse.from(
			partyApplication.getInvite(code, user == null ? null : user.userId())));
	}

	@Operation(summary = "초대 코드로 입장",
		description = "참가 방식과 상관없이 바로 멤버가 된다. B107 유효하지 않은 초대, 그 밖의 오류는 참가와 같다")
	@PostMapping("/invites/{code}/members")
	public ResponseEntity<PartyDetailResponse> joinByInvite(@CurrentUser AuthPrincipal user,
		@PathVariable String code, @Valid @RequestBody(required = false) PartyMemberCreateRequest request) {
		PartyMemberCreateRequest body = orEmpty(request);
		return ResponseEntity.ok(PartyDetailResponse.from(
			partyApplication.joinByInvite(user.userId(), code, body.positionCode(), body.tierCode())));
	}

	@Operation(summary = "파티 나가기", description = "방장이면 가장 먼저 들어온 멤버가 잇고, 마지막이면 해산한다. 멤버가 아니면 A102")
	@DeleteMapping("/{id}/members/me")
	public ResponseEntity<Void> leave(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		partyApplication.leave(user.userId(), id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "멤버 내보내기",
		description = "방장만(A101). 그 회원은 이 파티에 다시 들어오지 못한다. 멤버가 아니면 R001, 자기 자신이면 B113")
	@DeleteMapping("/{id}/members/{userId}")
	public ResponseEntity<Void> kick(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@PathVariable Long userId) {
		partyApplication.kick(user.userId(), id, userId);
		return ResponseEntity.noContent().build();
	}

	private static PartyMemberCreateRequest orEmpty(PartyMemberCreateRequest request) {
		return request == null ? new PartyMemberCreateRequest(null, null) : request;
	}
}
