package com.moa.api.controller.v1.party;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.party.PartyApplication;
import com.moa.api.controller.v1.party.request.PartyCreateRequest;
import com.moa.api.controller.v1.party.request.PartyJoinPolicyRequest;
import com.moa.api.controller.v1.party.request.PartyModifyRequest;
import com.moa.api.controller.v1.party.response.MyPartyResponse;
import com.moa.api.controller.v1.party.response.PartyDetailResponse;
import com.moa.api.controller.v1.party.response.PartyInviteResponse;
import com.moa.api.controller.v1.party.response.PartyListResponse;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 모집 페이지와 파티 (specs/008-party contracts/party-api.md). 목록과 상세는 로그인 없이 연다 (US2-AC6).
 * 방장만 하는 일은 파티마다의 관계라 권한 키가 아니라 A101로 막는다 (research.md R9).
 */
@Tag(name = "파티 API")
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class ApiPartyController {

	private final PartyApplication partyApplication;

	@Operation(summary = "모집 페이지", description = "모집 중인 공개·승인 파티만 최신순. 모드만 주고 게임이 없으면 V001")
	@PermitAll
	@SecurityRequirements
	@GetMapping
	public ResponseEntity<PageResponse<PartyListResponse>> getPage(
		@RequestParam(required = false) String gameCode,
		@RequestParam(required = false) String modeCode,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			partyApplication.getRecruitPage(gameCode, modeCode, PageRequest.of(page, size)), PartyListResponse::from));
	}

	@Operation(summary = "파티 만들기",
		description = "만든 회원이 방장이다. 칸이 잘못되면 V001, 이미 파티에 속하면 B103, 관리자 계정은 A103")
	@PostMapping
	public ResponseEntity<PartyDetailResponse> create(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody PartyCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(PartyDetailResponse.from(partyApplication.create(user.userId(), request.toCommand())));
	}

	@Operation(summary = "내 파티", description = "지금 속한 파티와 대기 중인 신청. 관리자 계정은 A103")
	@GetMapping("/me")
	public ResponseEntity<MyPartyResponse> me(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(MyPartyResponse.from(partyApplication.myParty(user.userId())));
	}

	@Operation(summary = "파티 상세", description = "비공개 파티는 멤버만. 없거나 해산했거나 멤버가 아닌 회원의 비공개 파티면 R001")
	@PermitAll
	@SecurityRequirements
	@GetMapping("/{id}")
	public ResponseEntity<PartyDetailResponse> get(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		return ResponseEntity.ok(PartyDetailResponse.from(partyApplication.get(id, user == null ? null
			: user.userId())));
	}

	@Operation(summary = "파티 정보 고치기",
		description = "방장만(A101). 인원이 지금보다 작으면 B109, 차지된 포지션을 빼거나 줄이면 B110, 멤버가 있는 포지션 파티의 게임 바꾸기는 B111")
	@PutMapping("/{id}")
	public ResponseEntity<PartyDetailResponse> modify(@CurrentUser AuthPrincipal user, @PathVariable Long id,
		@Valid @RequestBody PartyModifyRequest request) {
		return ResponseEntity.ok(PartyDetailResponse.from(
			partyApplication.modify(user.userId(), id, request.toCommand())));
	}

	@Operation(summary = "참가 방식 바꾸기", description = "방장만(A101). 승인에서 바뀌면 대기 신청을 모두 취소한다")
	@PutMapping("/{id}/join-policy")
	public ResponseEntity<PartyDetailResponse> changeJoinPolicy(@CurrentUser AuthPrincipal user,
		@PathVariable Long id, @Valid @RequestBody PartyJoinPolicyRequest request) {
		return ResponseEntity.ok(PartyDetailResponse.from(
			partyApplication.changeJoinPolicy(user.userId(), id, request.joinPolicy())));
	}

	@Operation(summary = "모집 마감", description = "방장만(A101). 자리가 나도 저절로 다시 열리지 않는다")
	@PostMapping("/{id}/close")
	public ResponseEntity<PartyDetailResponse> close(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		return ResponseEntity.ok(PartyDetailResponse.from(partyApplication.close(user.userId(), id)));
	}

	@Operation(summary = "모집 다시 열기", description = "방장만(A101). 정원이 찼으면 B101")
	@PostMapping("/{id}/reopen")
	public ResponseEntity<PartyDetailResponse> reopen(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		return ResponseEntity.ok(PartyDetailResponse.from(partyApplication.reopen(user.userId(), id)));
	}

	@Operation(summary = "초대 코드 새로 만들기", description = "방장만(A101). 이전 코드와 링크는 더 쓰지 못한다")
	@PostMapping("/{id}/invite-code")
	public ResponseEntity<PartyInviteResponse> regenerateInviteCode(@CurrentUser AuthPrincipal user,
		@PathVariable Long id) {
		return ResponseEntity.ok(PartyInviteResponse.from(partyApplication.regenerateInviteCode(user.userId(), id)));
	}

	@Operation(summary = "파티 해산", description = "방장만(A101). 멤버와 대기 신청이 사라지고 그 회원들에게 알린다")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> dissolve(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		partyApplication.dissolve(user.userId(), id);
		return ResponseEntity.noContent().build();
	}
}
