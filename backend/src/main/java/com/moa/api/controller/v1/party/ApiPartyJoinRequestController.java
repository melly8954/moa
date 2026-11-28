package com.moa.api.controller.v1.party;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.party.PartyJoinRequestApplication;
import com.moa.api.controller.v1.party.request.PartyJoinRequestCreateRequest;
import com.moa.api.controller.v1.party.response.PartyDetailResponse;
import com.moa.api.controller.v1.party.response.PartyJoinRequestCreateResponse;
import com.moa.api.controller.v1.party.response.PartyJoinRequestListResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 승인 파티의 참가 신청 (specs/008-party US5, contracts/party-api.md "승인 신청").
 */
@Tag(name = "파티 신청 API")
@RestController
@RequestMapping("/api/v1/parties/{id}/join-requests")
@RequiredArgsConstructor
public class ApiPartyJoinRequestController {

	private final PartyJoinRequestApplication partyJoinRequestApplication;

	@Operation(summary = "참가 신청",
		description = "승인 파티만(B112). B101 정원, B102 마감, B103 이미 속함, B104 참가 제한, B105·B106 포지션, B108 대기 중인 신청 있음")
	@PostMapping
	public ResponseEntity<PartyJoinRequestCreateResponse> apply(@CurrentUser AuthPrincipal user,
		@PathVariable Long id, @Valid @RequestBody(required = false) PartyJoinRequestCreateRequest request) {
		PartyJoinRequestCreateRequest body = request == null ? new PartyJoinRequestCreateRequest(null, null) : request;
		return ResponseEntity.status(HttpStatus.CREATED).body(new PartyJoinRequestCreateResponse(
			partyJoinRequestApplication.apply(user.userId(), id, body.positionCode(), body.tierCode())));
	}

	@Operation(summary = "신청 목록", description = "방장만(A101). 대기 중인 것만 신청순")
	@GetMapping
	public ResponseEntity<List<PartyJoinRequestListResponse>> getPending(@CurrentUser AuthPrincipal user,
		@PathVariable Long id) {
		return ResponseEntity.ok(partyJoinRequestApplication.getPending(user.userId(), id).stream()
			.map(PartyJoinRequestListResponse::from)
			.toList());
	}

	@Operation(summary = "신청 승인",
		description = "방장만(A101). 마감 중에도 자리가 있으면 된다. B101 정원, B105 포지션 참, B103 그사이 다른 파티에 들어감, 대기 중이 아니면 R001")
	@PostMapping("/{joinRequestId}/approve")
	public ResponseEntity<PartyDetailResponse> approve(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@PathVariable Long joinRequestId) {
		return ResponseEntity.ok(PartyDetailResponse.from(
			partyJoinRequestApplication.approve(user.userId(), id, joinRequestId)));
	}

	@Operation(summary = "신청 거절", description = "방장만(A101). 그 회원은 이 파티에 다시 들어오지 못한다. 대기 중이 아니면 R001")
	@PostMapping("/{joinRequestId}/reject")
	public ResponseEntity<Void> reject(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@PathVariable Long joinRequestId) {
		partyJoinRequestApplication.reject(user.userId(), id, joinRequestId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "내 신청 취소", description = "대기 중인 내 신청이 없으면 R001")
	@DeleteMapping("/me")
	public ResponseEntity<Void> cancel(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		partyJoinRequestApplication.cancel(user.userId(), id);
		return ResponseEntity.noContent().build();
	}
}
