package com.moa.api.service.party;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.party.DissolveResult;
import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.MyJoinRequestDto;
import com.moa.api.dto.party.MyPartySnapshot;
import com.moa.api.dto.party.PartyCodeBook;
import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyForm;
import com.moa.api.dto.party.PartyInviteSnapshot;
import com.moa.api.dto.party.PartyJoinRequestDto;
import com.moa.api.dto.party.PartyMemberDto;
import com.moa.api.dto.party.PartyPositionDto;
import com.moa.api.dto.party.PartyPositionSlot;
import com.moa.api.dto.party.PartyRelation;
import com.moa.api.dto.party.PartySnapshot;
import com.moa.api.dto.party.PartySummarySnapshot;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyJoinRequest;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyPosition;
import com.moa.api.entity.party.PartySettings;
import com.moa.api.repository.reader.party.PartyReader;
import com.moa.api.repository.writer.party.PartyWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 파티 조회, 만들기, 방장의 관리 (specs/008-party). 방장의 쓰기는 파티 락 안에서 방장 확인(A101)을 먼저 한다 (research.md R6).
 */
@Service
@RequiredArgsConstructor
public class PartyService {

	/** 초대 코드 글자. 헷갈리는 0 O 1 I는 뺀다 (data-model.md tb_party.invite_code) */
	private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int INVITE_LENGTH = 8;
	private static final int MIN_CAPACITY = 2;
	private static final int MAX_CAPACITY = 10;
	/** 롤은 포지션마다 1자리다 (FR-022) */
	private static final String LOL = "LOL";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final PartyReader partyReader;
	private final PartyWriter partyWriter;

	/**
	 * 파티 화면 한 장. 비공개 파티는 멤버에게만 보인다 (FR-014).
	 *
	 * @param viewerUserId 로그인하지 않았으면 null
	 * @throws ServiceException 없거나 해산했거나, 멤버가 아닌 회원의 비공개 파티면 R001
	 */
	public PartySnapshot getSnapshot(Long partyId, Long viewerUserId) {
		PartySnapshot snapshot = snapshot(requireParty(partyId), viewerUserId);
		if (snapshot.party().joinPolicy() == JoinPolicy.PRIVATE && !isMember(snapshot.relation())) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
		return snapshot;
	}

	/**
	 * 멤버에게 보낼 파티 한 장 (실시간 이벤트). 보는 회원이 없고 비공개 파티도 읽는다.
	 *
	 * @throws ServiceException 없거나 해산했으면 R001
	 */
	public PartySnapshot getSnapshotForMembers(Long partyId) {
		return snapshot(requireParty(partyId), null);
	}

	/**
	 * 모집 페이지 (FR-004). 모집 중인 공개·승인 파티만 최신순이다.
	 *
	 * @throws ServiceException 게임 없이 모드만 주면 V001
	 */
	public Page<PartySummarySnapshot> getRecruitPage(String gameCode, String modeCode, Pageable pageable) {
		String game = blankToNull(gameCode);
		String mode = blankToNull(modeCode);
		if (game == null && mode != null) {
			throw invalid(List.of(new FieldError("modeCode", "모드는 게임을 고른 뒤에 거를 수 있습니다.")));
		}
		Page<Party> page = partyReader.findRecruitPage(game, mode, pageable);
		Map<Long, PartySummarySnapshot> summaries = summaries(page.getContent());
		return page.map(party -> summaries.get(party.getId()));
	}

	/**
	 * 파티의 게임 코드 (티어 확인에 쓴다).
	 *
	 * @throws ServiceException 없거나 해산했으면 R001
	 */
	public String getGameCode(Long partyId) {
		return requireParty(partyId).getGameCode();
	}

	/**
	 * 초대 코드의 파티.
	 *
	 * @throws ServiceException 없는 코드, 바뀐 코드, 해산한 파티면 B107
	 */
	public Long getIdByInviteCode(String inviteCode) {
		return requireInvite(inviteCode).getId();
	}

	/**
	 * 초대 입장 화면 (contracts/party-api.md InvitePreview). 이유는 admit의 확인 순서와 같다.
	 *
	 * @param viewerUserId 로그인하지 않았으면 null
	 * @throws ServiceException 없는 코드, 바뀐 코드, 해산한 파티면 B107
	 */
	public PartyInviteSnapshot getInvite(String inviteCode, Long viewerUserId) {
		Party party = requireInvite(inviteCode);
		PartySnapshot snapshot = snapshot(party, viewerUserId);
		String reason = null;
		if (snapshot.relation() == PartyRelation.BANNED) {
			reason = ErrorCode.PARTY_BANNED.getCode();
		} else if (viewerUserId != null && partyReader.findMembership(viewerUserId).isPresent()) {
			reason = ErrorCode.PARTY_ALREADY_JOINED.getCode();
		} else if (snapshot.members().size() >= party.getCapacity()) {
			reason = ErrorCode.PARTY_FULL.getCode();
		} else if (!party.isRecruiting()) {
			reason = ErrorCode.PARTY_CLOSED.getCode();
		}
		return new PartyInviteSnapshot(snapshot, reason);
	}

	/**
	 * 내 파티 (US8). 해산한 파티와 그 신청은 빠진다 (FR-016).
	 */
	public MyPartySnapshot getMine(Long userId) {
		PartySummarySnapshot party = null;
		PartyMember membership = partyReader.findMembership(userId).orElse(null);
		if (membership != null) {
			party = partyReader.find(membership.getPartyId())
				.map(found -> summaries(List.of(found)).get(found.getId()))
				.orElse(null);
		}
		List<PartyJoinRequest> requests = partyReader.findPendingRequestsOf(userId);
		Map<Long, Party> parties = partyReader.findAll(requests.stream().map(PartyJoinRequest::getPartyId).toList())
			.stream()
			.collect(Collectors.toMap(Party::getId, Function.identity()));
		List<MyJoinRequestDto> joinRequests = requests.stream()
			.filter(request -> parties.containsKey(request.getPartyId()))
			.map(request -> new MyJoinRequestDto(PartyJoinRequestDto.from(request),
				PartyDto.from(parties.get(request.getPartyId()))))
			.toList();
		return new MyPartySnapshot(party, party == null ? null : membership.getRole(), joinRequests);
	}

	/**
	 * 파티를 만든다 (US1, US10). 방장 멤버 행은 이어서 Application이 PartyMemberService.addHost로 넣는다.
	 *
	 * @param codes 고를 수 있는 공통코드 (Application이 읽는다)
	 * @throws ServiceException 이미 파티에 속하면 B103, 칸이 잘못되면 V001
	 */
	public PartyDto create(Long userId, PartyForm form, JoinPolicy joinPolicy, PartyCodeBook codes) {
		if (partyReader.findMembership(userId).isPresent()) {
			throw new ServiceException(ErrorCode.PARTY_ALREADY_JOINED);
		}
		Conditions conditions = validate(form, joinPolicy, codes, null);
		Party party = partyWriter.save(new Party(conditions.settings(), joinPolicy, userId, newInviteCode()));
		partyWriter.savePositions(conditions.positions().entrySet().stream()
			.map(slot -> new PartyPosition(party.getId(), slot.getKey(), slot.getValue()))
			.toList());
		return PartyDto.from(party);
	}

	/**
	 * 방장이 파티 정보를 고친다 (US7-AC1, US10-AC10). 인원을 지금 인원과 같게 하면 자동 마감하고, 자리가 나면 다시 모집한다.
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101, 칸이 잘못되면 V001, 인원이 지금보다 작으면 B109,
	 *     차지된 포지션을 빼거나 줄이면 B110, 멤버가 있는 포지션 파티의 게임을 바꾸면 B111
	 */
	public PartyDto modify(Long partyId, Long userId, PartyForm form, PartyCodeBook codes) {
		Party party = requireHostLocked(partyId, userId);
		Conditions conditions = validate(form, party.getJoinPolicy(), codes, party);
		PartySettings settings = conditions.settings();
		List<PartyMember> members = partyReader.findMembers(partyId);
		if (settings.capacity() < members.size()) {
			throw new ServiceException(ErrorCode.PARTY_CAPACITY_BELOW_MEMBERS);
		}
		if (party.isPositionEnabled() && members.size() > 1 && !party.getGameCode().equals(settings.gameCode())) {
			throw new ServiceException(ErrorCode.PARTY_GAME_LOCKED);
		}
		String hostPosition = settings.positionEnabled() ? blankToNull(form.hostPositionCode()) : null;
		requireMembersFit(members, conditions.positions(), settings.positionEnabled(), hostPosition);

		party.modify(settings);
		syncPositions(partyId, conditions.positions());
		members.stream().filter(PartyMember::isHost).forEach(host -> host.changePosition(hostPosition));
		if (members.size() >= party.getCapacity()) {
			if (party.isRecruiting()) {
				party.close(false);
			}
		} else if (!party.isRecruiting() && !party.isClosedByHost()) {
			party.reopen();
		}
		return PartyDto.from(party);
	}

	/**
	 * 참가 방식을 바꾼다 (US7-AC2~AC4). 승인에서 다른 방식으로 바뀌면 대기 신청을 모두 취소한다 (FR-005).
	 *
	 * @return 대기 신청을 취소했는지
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101
	 */
	public boolean changeJoinPolicy(Long partyId, Long userId, JoinPolicy joinPolicy) {
		Party party = requireHostLocked(partyId, userId);
		boolean cancel = party.getJoinPolicy() == JoinPolicy.APPROVAL && joinPolicy != JoinPolicy.APPROVAL;
		if (cancel) {
			LocalDateTime now = LocalDateTime.now();
			partyReader.findPendingRequests(partyId).forEach(request -> request.cancel(now));
		}
		party.changeJoinPolicy(joinPolicy);
		return cancel;
	}

	/**
	 * 방장이 모집을 마감한다 (US7-AC6). 자리가 나도 저절로 다시 열리지 않는다 (FR-008).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101
	 */
	public void close(Long partyId, Long userId) {
		requireHostLocked(partyId, userId).close(true);
	}

	/**
	 * 방장이 모집을 다시 연다 (US7-AC6).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101, 정원이 찼으면 B101
	 */
	public void reopen(Long partyId, Long userId) {
		Party party = requireHostLocked(partyId, userId);
		if (partyReader.countMembers(partyId) >= party.getCapacity()) {
			throw new ServiceException(ErrorCode.PARTY_FULL);
		}
		party.reopen();
	}

	/**
	 * 초대 코드를 새로 만든다. 이전 코드는 더 쓰지 못한다 (US7-AC7).
	 *
	 * @return 새 코드
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101
	 */
	public String regenerateInviteCode(Long partyId, Long userId) {
		Party party = requireHostLocked(partyId, userId);
		party.regenerateInviteCode(newInviteCode());
		return party.getInviteCode();
	}

	/**
	 * 방장이 해산한다 (US7-AC8). 받을 사람은 멤버·신청을 지우기 전에 모은다 (FR-029).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101
	 */
	public DissolveResult dissolve(Long partyId, Long userId) {
		Party party = requireHostLocked(partyId, userId);
		Set<Long> recipients = new LinkedHashSet<>();
		partyReader.findMembers(partyId).stream()
			.map(PartyMember::getUserId)
			.filter(memberId -> !memberId.equals(userId))
			.forEach(recipients::add);
		partyReader.findPendingRequests(partyId).forEach(request -> recipients.add(request.getUserId()));
		partyWriter.dissolve(party, userId);
		return new DissolveResult(partyId, party.getTitle(), recipients);
	}

	/**
	 * 들어오는 회원이 적은 티어가 그 게임의 티어인지 본다 (FR-024). 범위 밖이어도 막지 않는다.
	 *
	 * @throws ServiceException 그 게임의 티어가 아니면 V001
	 */
	public void requireTier(String tierCode, PartyCodeBook codes) {
		if (tierCode != null && !codes.tiers().containsKey(tierCode)) {
			throw invalid(List.of(new FieldError("tierCode", "이 게임에 없는 티어입니다.")));
		}
	}

	private PartySnapshot snapshot(Party party, Long viewerUserId) {
		Long partyId = party.getId();
		List<PartyMember> members = partyReader.findMembers(partyId);
		boolean member = viewerUserId != null
			&& members.stream().anyMatch(m -> m.getUserId().equals(viewerUserId));
		Long joinRequestId = null;
		PartyRelation relation = PartyRelation.NONE;
		if (member) {
			relation = party.isHost(viewerUserId) ? PartyRelation.HOST : PartyRelation.MEMBER;
		} else if (viewerUserId != null) {
			if (partyReader.isBanned(partyId, viewerUserId)) {
				relation = PartyRelation.BANNED;
			} else {
				joinRequestId = partyReader.findPendingRequest(partyId, viewerUserId)
					.map(PartyJoinRequest::getId)
					.orElse(null);
				relation = joinRequestId == null ? PartyRelation.NONE : PartyRelation.PENDING;
			}
		}
		return new PartySnapshot(PartyDto.from(party), positions(partyReader.findPositions(partyId), members),
			members.stream().map(PartyMemberDto::from).toList(), relation, joinRequestId);
	}

	private static boolean isMember(PartyRelation relation) {
		return relation == PartyRelation.MEMBER || relation == PartyRelation.HOST;
	}

	/** 카드 여러 장. 포지션과 멤버를 한 번에 읽는다 */
	private Map<Long, PartySummarySnapshot> summaries(List<Party> parties) {
		List<Long> ids = parties.stream().map(Party::getId).toList();
		Map<Long, List<PartyPosition>> positions = partyReader.findPositions(ids).stream()
			.collect(Collectors.groupingBy(PartyPosition::getPartyId));
		Map<Long, List<PartyMember>> members = partyReader.findMembers(ids).stream()
			.collect(Collectors.groupingBy(PartyMember::getPartyId));
		Map<Long, PartySummarySnapshot> summaries = new HashMap<>();
		for (Party party : parties) {
			List<PartyMember> partyMembers = members.getOrDefault(party.getId(), List.of());
			List<PartyPosition> partyPositions = positions.getOrDefault(party.getId(), List.of()).stream()
				.sorted((a, b) -> Long.compare(a.getId(), b.getId()))
				.toList();
			summaries.put(party.getId(), new PartySummarySnapshot(PartyDto.from(party), partyMembers.size(),
				positions(partyPositions, partyMembers)));
		}
		return summaries;
	}

	/** 포지션마다 자리와 차지한 수 */
	static List<PartyPositionDto> positions(List<PartyPosition> positions, List<PartyMember> members) {
		return positions.stream()
			.map(position -> PartyPositionDto.of(position,
				(int)PartyPositions.filled(members, position.getPositionCode())))
			.toList();
	}

	/** 검증을 마친 칸과 포지션 자리 (포지션 코드 → 자리 수, 고른 순서) */
	private record Conditions(PartySettings settings, Map<String, Integer> positions) {
	}

	/**
	 * 칸을 확인한다. 맞지 않는 칸은 모아 한 번에 V001로 알린다 (US1-AC3, US10).
	 *
	 * @param current 고치는 파티. 만들 때는 null. 게임을 바꾸지 않으면 그 게임이 꺼져 있어도 그대로 둔다
	 */
	private Conditions validate(PartyForm form, JoinPolicy joinPolicy, PartyCodeBook codes, Party current) {
		List<FieldError> errors = new ArrayList<>();
		if (joinPolicy == null) {
			errors.add(new FieldError("joinPolicy", "참가 방식을 골라 주세요."));
		}
		String game = blankToNull(form.gameCode());
		boolean sameGame = current != null && current.getGameCode().equals(game);
		if (game == null || !sameGame && !codes.games().contains(game)) {
			errors.add(new FieldError("gameCode", "게임 목록에 없는 게임입니다."));
		}
		String mode = blankToNull(form.modeCode());
		if (mode != null && !codes.modes().contains(mode)) {
			errors.add(new FieldError("modeCode", "이 게임에 없는 모드입니다."));
		}
		String tierMin = tier(form.tierMinCode(), "tierMinCode", codes, errors);
		String tierMax = tier(form.tierMaxCode(), "tierMaxCode", codes, errors);
		if (tierMin != null && tierMax != null && codes.tiers().get(tierMin) > codes.tiers().get(tierMax)) {
			errors.add(new FieldError("tierMaxCode", "최대 티어는 최소 티어보다 낮을 수 없습니다."));
		}
		List<PartyPositionSlot> slots = form.positions() == null ? List.of() : form.positions();
		String hostPosition = blankToNull(form.hostPositionCode());
		Map<String, Integer> positions = new LinkedHashMap<>();
		int capacity;
		if (slots.isEmpty()) {
			if (hostPosition != null) {
				errors.add(new FieldError("hostPositionCode", "모집할 포지션을 함께 골라 주세요."));
			}
			capacity = form.capacity() == null ? 0 : form.capacity();
			if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
				errors.add(new FieldError("capacity", "모집 인원은 2~10명입니다."));
			}
		} else {
			positions = positions(game, slots, hostPosition, codes, errors);
			capacity = positions.values().stream().mapToInt(Integer::intValue).sum();
			if (!positions.isEmpty() && (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY)) {
				errors.add(new FieldError("positions", "포지션 자리의 합(방장 포함)은 2~10이어야 합니다."));
			}
		}
		if (!errors.isEmpty()) {
			throw invalid(errors);
		}
		String description = form.description() == null ? "" : form.description();
		return new Conditions(new PartySettings(game, mode, tierMin, tierMax, form.title(), description, capacity,
			!positions.isEmpty()), positions);
	}

	/**
	 * 포지션 자리를 확인한다 (FR-022). 방장의 포지션이 목록에 없으면 1자리를 더한다 (롤은 모집할 포지션만 고른다).
	 */
	private static Map<String, Integer> positions(String game, List<PartyPositionSlot> slots, String hostPosition,
		PartyCodeBook codes, List<FieldError> errors) {
		Map<String, Integer> positions = new LinkedHashMap<>();
		if (codes.positions().isEmpty()) {
			errors.add(new FieldError("positions", "이 게임은 포지션을 고를 수 없습니다."));
			return positions;
		}
		for (PartyPositionSlot slot : slots) {
			String code = blankToNull(slot.positionCode());
			int count = slot.slots() == null ? 0 : slot.slots();
			if (code == null || !codes.positions().contains(code)) {
				errors.add(new FieldError("positions", "이 게임에 없는 포지션입니다."));
			} else if (positions.containsKey(code)) {
				errors.add(new FieldError("positions", "같은 포지션을 두 번 고를 수 없습니다."));
			} else if (count < 1) {
				errors.add(new FieldError("positions", "포지션 자리는 1 이상이어야 합니다."));
			} else if (LOL.equals(game) && count != 1) {
				errors.add(new FieldError("positions", "롤은 포지션마다 1자리입니다."));
			} else {
				positions.put(code, count);
			}
		}
		if (hostPosition == null) {
			errors.add(new FieldError("hostPositionCode", "내 포지션을 골라 주세요."));
		} else if (!codes.positions().contains(hostPosition)) {
			errors.add(new FieldError("hostPositionCode", "이 게임에 없는 포지션입니다."));
		} else {
			positions.putIfAbsent(hostPosition, 1);
		}
		return positions;
	}

	private static String tier(String value, String field, PartyCodeBook codes, List<FieldError> errors) {
		String tier = blankToNull(value);
		if (tier != null && !codes.tiers().containsKey(tier)) {
			errors.add(new FieldError(field, "이 게임에 없는 티어입니다."));
			return null;
		}
		return tier;
	}

	/**
	 * 고친 포지션 자리에 지금 멤버가 모두 들어가는지 본다 (US10-AC10). 방장은 새로 고른 포지션으로 센다.
	 *
	 * @throws ServiceException 차지된 포지션을 빼거나 그 자리를 멤버 수보다 줄이면 B110
	 */
	private static void requireMembersFit(List<PartyMember> members, Map<String, Integer> positions,
		boolean positionEnabled, String hostPosition) {
		Map<String, Integer> filled = new HashMap<>();
		for (PartyMember member : members) {
			String code = member.isHost() ? hostPosition : member.getPositionCode();
			if (!positionEnabled) {
				// 포지션 조건을 끄면 방장 말고 포지션을 차지한 멤버가 없어야 한다
				if (!member.isHost() && code != null) {
					throw new ServiceException(ErrorCode.PARTY_POSITION_OCCUPIED);
				}
				continue;
			}
			if (code == null || !positions.containsKey(code)) {
				throw new ServiceException(ErrorCode.PARTY_POSITION_OCCUPIED);
			}
			filled.merge(code, 1, Integer::sum);
		}
		filled.forEach((code, count) -> {
			if (count > positions.get(code)) {
				throw new ServiceException(ErrorCode.PARTY_POSITION_OCCUPIED);
			}
		});
	}

	/**
	 * 포지션 자리를 고친 값으로 맞춘다. 있는 행은 자리 수만 바꾸고, 빠진 것은 지우고, 새것은 넣는다.
	 * 지우고 다시 넣으면 같은 플러시에서 넣기가 먼저 돌아 유니크(party_id, position_code)에 걸린다.
	 */
	private void syncPositions(Long partyId, Map<String, Integer> positions) {
		List<PartyPosition> existing = partyReader.findPositions(partyId);
		List<PartyPosition> removed = new ArrayList<>();
		Set<String> kept = new LinkedHashSet<>();
		for (PartyPosition position : existing) {
			Integer slots = positions.get(position.getPositionCode());
			if (slots == null) {
				removed.add(position);
			} else {
				position.changeSlots(slots);
				kept.add(position.getPositionCode());
			}
		}
		partyWriter.deletePositions(removed);
		partyWriter.savePositions(positions.entrySet().stream()
			.filter(slot -> !kept.contains(slot.getKey()))
			.map(slot -> new PartyPosition(partyId, slot.getKey(), slot.getValue()))
			.toList());
	}

	private Party requireParty(Long partyId) {
		return partyReader.find(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private Party requireInvite(String inviteCode) {
		String code = blankToNull(inviteCode);
		return (code == null ? Optional.<Party>empty()
			: partyReader.findByInviteCode(code.toUpperCase(Locale.ROOT)))
			.orElseThrow(() -> new ServiceException(ErrorCode.PARTY_INVITE_INVALID));
	}

	private Party requireHostLocked(Long partyId, Long userId) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (!party.isHost(userId)) {
			throw new ServiceException(ErrorCode.PARTY_HOST_ONLY);
		}
		return party;
	}

	/** 살아 있는 파티끼리 겹치지 않는 초대 코드 (uk_party_invite_code_active_key) */
	private String newInviteCode() {
		String code;
		do {
			StringBuilder builder = new StringBuilder(INVITE_LENGTH);
			for (int i = 0; i < INVITE_LENGTH; i++) {
				builder.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
			}
			code = builder.toString();
		} while (partyReader.existsByInviteCode(code));
		return code;
	}

	private static ServiceException invalid(List<FieldError> errors) {
		return new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
