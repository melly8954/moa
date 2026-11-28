package com.moa.support;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.moa.common.security.Role;

/**
 * 파티 인수 테스트의 처음 상태를 만든다 (specs/008-party tasks T041). 회원은 TestUsers로 만들고, 파티와 멤버·신청·메시지는
 * API를 거치지 않고 JdbcTemplate으로 바로 넣는다. 확인하려는 동작만 API로 부르게 하려는 것이다.
 * DatabaseCleaner가 테스트마다 표를 비우므로 테스트 안에서 만든다. 공통코드는 CodeSeed.restore()로 따로 되돌린다.
 */
@Component
public class PartyFixtures {

	/** 시스템 계정. 마이그레이션 시드와 감사 열(created_by)이 가리킨다 */
	public static final long SYSTEM_ID = 1L;

	/** 초대 코드에 쓰는 글자 (0 O 1 I 제외, data-model.md tb_party.invite_code) */
	private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final JdbcTemplate jdbcTemplate;
	private final TestUsers testUsers;
	/**
	 * 들어온 시각을 넣은 순서대로 벌린다. 방장을 잇는 순서(joined_at)가 같은 시각으로 겹치지 않게 한다.
	 * 한 시간 앞에서 시작해, 테스트 안에서 API로 들어온 멤버(지금 시각)는 늘 그 뒤다
	 */
	private final AtomicLong joinSequence = new AtomicLong();

	public PartyFixtures(JdbcTemplate jdbcTemplate, TestUsers testUsers) {
		this.jdbcTemplate = jdbcTemplate;
		this.testUsers = testUsers;
	}

	/** 시스템 계정(id 1)을 만든다. 이미 있으면 그대로 둔다 */
	public void system() {
		testUsers.create(SYSTEM_ID, "system", null, Role.USER);
	}

	/** 회원(USER) 계정을 만든다. 표시 이름(tb_user.name)과 로그인 아이디는 "member{id}"다. 시스템 계정도 함께 만든다 */
	public void member(long userId) {
		system();
		testUsers.create(userId, displayName(userId), null, Role.USER);
	}

	/** 회원 여러 명 */
	public void members(long... userIds) {
		for (long userId : userIds) {
			member(userId);
		}
	}

	/** 관리자 계정(ADMIN)을 만든다. 파티를 쓸 수 없는 계정이다 (FR-012) */
	public void admin(long userId) {
		system();
		testUsers.create(userId, "admin" + userId, null, Role.ADMIN);
	}

	/** member(userId)로 만든 회원의 표시 이름 */
	public static String displayName(long userId) {
		return "member" + userId;
	}

	/** 롤(LOL), 모드·티어·포지션 조건 없는 공개 파티. 방장 계정도 만든다. 파티 ID */
	public long openParty(long hostUserId, int capacity) {
		return party(hostUserId).capacity(capacity).create();
	}

	/** 승인 파티 */
	public long approvalParty(long hostUserId, int capacity) {
		return party(hostUserId).joinPolicy("APPROVAL").capacity(capacity).create();
	}

	/** 비공개 파티 */
	public long privateParty(long hostUserId, int capacity) {
		return party(hostUserId).joinPolicy("PRIVATE").capacity(capacity).create();
	}

	/**
	 * 롤 포지션 파티. 방장 포지션과 모집할 포지션마다 1자리이고, 인원은 자리의 합이다 (contracts/party-api.md "만들기").
	 *
	 * @param openPositions 모집할 포지션 (방장 포지션 제외)
	 */
	public long lolPositionParty(long hostUserId, String hostPositionCode, String... openPositions) {
		PartyBuilder builder = party(hostUserId).hostPosition(hostPositionCode).position(hostPositionCode, 1);
		for (String positionCode : openPositions) {
			builder.position(positionCode, 1);
		}
		return builder.create();
	}

	/** 파티를 골라 만든다. 기본값은 롤, 공개, 모집 중, 인원 5, 제목 "파티 {방장 ID}"다 */
	public PartyBuilder party(long hostUserId) {
		return new PartyBuilder(hostUserId);
	}

	/** 멤버(MEMBER)를 더한다. 회원 계정도 만든다. 들어온 순서는 부른 순서다 */
	public void addMember(long partyId, long userId) {
		addMember(partyId, userId, null);
	}

	/** 포지션을 차지한 멤버를 더한다 */
	public void addMember(long partyId, long userId, String positionCode) {
		member(userId);
		insertMember(partyId, userId, "MEMBER", positionCode);
	}

	/** 멤버 여러 명을 차례로 더한다 */
	public void addMembers(long partyId, long... userIds) {
		for (long userId : userIds) {
			addMember(partyId, userId);
		}
	}

	/** 지금 멤버 수 */
	public int memberCount(long partyId) {
		Integer count = jdbcTemplate.queryForObject("select count(*) from tb_party_member where party_id = ?",
			Integer.class, partyId);
		return count == null ? 0 : count;
	}

	/** 그 회원의 멤버 행이 있는지 (파티를 가리지 않는다) */
	public boolean isInAnyParty(long userId) {
		Integer count = jdbcTemplate.queryForObject("select count(*) from tb_party_member where user_id = ?",
			Integer.class, userId);
		return count != null && count > 0;
	}

	/** 대기(PENDING) 참가 신청을 넣는다. 회원 계정도 만든다. 신청 ID */
	public long pendingRequest(long partyId, long userId) {
		return pendingRequest(partyId, userId, null);
	}

	/** 포지션을 고른 대기 참가 신청 */
	public long pendingRequest(long partyId, long userId, String positionCode) {
		member(userId);
		jdbcTemplate.update("insert into tb_party_join_request (party_id, user_id, position_code, tier_code, status, "
			+ "decided_at, created_at, updated_at, created_by, updated_by) values (?, ?, ?, null, 'PENDING', null, "
			+ "now(6), now(6), ?, ?)", partyId, userId, positionCode, userId, userId);
		return lastId("tb_party_join_request");
	}

	/** 참가 신청의 상태 (PENDING, APPROVED, REJECTED, CANCELLED) */
	public String requestStatus(long joinRequestId) {
		return jdbcTemplate.queryForObject("select status from tb_party_join_request where id = ?", String.class,
			joinRequestId);
	}

	/** 그 회원의 참가 신청 행 수 (상태를 가리지 않는다) */
	public int requestCount(long userId) {
		Integer count = jdbcTemplate.queryForObject("select count(*) from tb_party_join_request where user_id = ?",
			Integer.class, userId);
		return count == null ? 0 : count;
	}

	/** 참가 제한을 넣는다 (KICKED, REJECTED) */
	public void ban(long partyId, long userId, String reason) {
		member(userId);
		jdbcTemplate.update("insert into tb_party_ban (party_id, user_id, reason, created_at, updated_at, created_by, "
			+ "updated_by) values (?, ?, ?, now(6), now(6), 1, 1)", partyId, userId, reason);
	}

	/** 파티를 해산한 상태로 만든다 (소프트 삭제. 멤버와 대기 신청도 지운다, data-model.md "상태 전이") */
	public void dissolve(long partyId) {
		jdbcTemplate.update("update tb_party set deleted_at = now(6), deleted_by = host_user_id where id = ?",
			partyId);
		jdbcTemplate.update("delete from tb_party_member where party_id = ?", partyId);
		jdbcTemplate.update("delete from tb_party_join_request where party_id = ? and status = 'PENDING'", partyId);
	}

	/** 모집 상태를 바꾼다 */
	public void status(long partyId, String status, boolean closedByHost) {
		jdbcTemplate.update("update tb_party set status = ?, closed_by_host = ? where id = ?", status, closedByHost,
			partyId);
	}

	/** 파티의 초대 코드 */
	public String inviteCode(long partyId) {
		return jdbcTemplate.queryForObject("select invite_code from tb_party where id = ?", String.class, partyId);
	}

	/**
	 * 채팅 메시지(CHAT)를 n개 넣는다. 내용은 "메시지 1"부터 "메시지 n"이고 id가 늘어나는 순서다.
	 *
	 * @return 넣은 메시지의 id. 넣은 순서
	 */
	public long[] chatMessages(long partyId, long senderUserId, int count) {
		long[] ids = new long[count];
		for (int i = 0; i < count; i++) {
			jdbcTemplate.update("insert into tb_party_message (party_id, type, sender_user_id, content, system_event, "
				+ "subject_user_id, created_at, updated_at, created_by, updated_by) values (?, 'CHAT', ?, ?, null, "
				+ "null, now(6), now(6), ?, ?)", partyId, senderUserId, "메시지 " + (i + 1), senderUserId,
				senderUserId);
			ids[i] = lastId("tb_party_message");
		}
		return ids;
	}

	/** 그 파티의 SYSTEM 메시지 안내 종류 (JOINED, LEFT, KICKED, HOST_CHANGED). id 순서 */
	public List<String> systemEvents(long partyId) {
		return jdbcTemplate.queryForList("select system_event from tb_party_message where party_id = ? and type = "
			+ "'SYSTEM' order by id", String.class, partyId);
	}

	private void insertMember(long partyId, long userId, String role, String positionCode) {
		jdbcTemplate.update(
			"insert into tb_party_member (party_id, user_id, role, position_code, tier_code, joined_at, "
				+ "created_at, updated_at, created_by, updated_by) values (?, ?, ?, ?, null, "
				+ "date_add(date_sub(now(6), interval 1 hour), interval ? microsecond), now(6), now(6), ?, ?)",
			partyId, userId, role, positionCode, joinSequence.incrementAndGet() * 1000, userId, userId);
	}

	private long lastId(String table) {
		Long id = jdbcTemplate.queryForObject("select max(id) from " + table, Long.class);
		if (id == null) {
			throw new IllegalStateException(table + "에 넣은 행이 없습니다");
		}
		return id;
	}

	private static String randomInviteCode() {
		StringBuilder code = new StringBuilder(8);
		for (int i = 0; i < 8; i++) {
			code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
		}
		return code.toString();
	}

	/** 파티 한 줄을 골라 넣는다. create()가 방장 계정, 파티, 포지션, 방장 멤버 행을 넣고 파티 ID를 돌려준다 */
	public final class PartyBuilder {

		private final long hostUserId;
		private String gameCode = "LOL";
		private String modeCode;
		private String tierMinCode;
		private String tierMaxCode;
		private String title;
		private String description = "";
		private Integer capacity = 5;
		private String joinPolicy = "OPEN";
		private String status = "RECRUITING";
		private boolean closedByHost;
		private String hostPositionCode;
		private String inviteCode;
		private final Map<String, Integer> positions = new LinkedHashMap<>();

		private PartyBuilder(long hostUserId) {
			this.hostUserId = hostUserId;
			this.title = "파티 " + hostUserId;
		}

		public PartyBuilder game(String gameCode) {
			this.gameCode = gameCode;
			return this;
		}

		public PartyBuilder mode(String modeCode) {
			this.modeCode = modeCode;
			return this;
		}

		public PartyBuilder tiers(String tierMinCode, String tierMaxCode) {
			this.tierMinCode = tierMinCode;
			this.tierMaxCode = tierMaxCode;
			return this;
		}

		public PartyBuilder title(String title) {
			this.title = title;
			return this;
		}

		public PartyBuilder description(String description) {
			this.description = description;
			return this;
		}

		/** 포지션이 없을 때의 모집 인원. 포지션을 더하면 자리의 합으로 바뀐다 */
		public PartyBuilder capacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		/** OPEN, APPROVAL, PRIVATE */
		public PartyBuilder joinPolicy(String joinPolicy) {
			this.joinPolicy = joinPolicy;
			return this;
		}

		/** 모집 마감(CLOSED). closedByHost는 방장이 직접 마감했는지 */
		public PartyBuilder closed(boolean closedByHost) {
			this.status = "CLOSED";
			this.closedByHost = closedByHost;
			return this;
		}

		/** 포지션 자리. 방장 포지션도 여기에 넣는다 (방장이 차지한다) */
		public PartyBuilder position(String positionCode, int slots) {
			positions.merge(positionCode, slots, Integer::sum);
			return this;
		}

		/** 방장이 차지한 포지션 */
		public PartyBuilder hostPosition(String positionCode) {
			this.hostPositionCode = positionCode;
			return this;
		}

		public PartyBuilder inviteCode(String inviteCode) {
			this.inviteCode = inviteCode;
			return this;
		}

		public long create() {
			member(hostUserId);
			int partyCapacity = positions.isEmpty() ? capacity
				: positions.values().stream().mapToInt(Integer::intValue).sum();
			jdbcTemplate.update("insert into tb_party (game_code, mode_code, tier_min_code, tier_max_code, title, "
				+ "description, capacity, join_policy, status, closed_by_host, host_user_id, position_enabled, "
				+ "invite_code, created_at, updated_at, created_by, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, now(6), now(6), ?, ?)",
				gameCode, modeCode, tierMinCode, tierMaxCode, title, description, partyCapacity, joinPolicy, status,
				closedByHost, hostUserId, !positions.isEmpty(), inviteCode == null ? randomInviteCode() : inviteCode,
				hostUserId, hostUserId);
			long partyId = lastId("tb_party");
			positions.forEach((positionCode, slots) -> jdbcTemplate.update("insert into tb_party_position (party_id, "
				+ "position_code, slots, created_at, updated_at, created_by, updated_by) values (?, ?, ?, now(6), "
				+ "now(6), ?, ?)", partyId, positionCode, slots, hostUserId, hostUserId));
			insertMember(partyId, hostUserId, "HOST", positions.isEmpty() ? null : hostPositionCode);
			return partyId;
		}
	}
}
