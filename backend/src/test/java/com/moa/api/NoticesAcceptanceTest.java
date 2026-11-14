package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.support.AcceptanceTest;

/**
 * 공지사항. 스펙 specs/006-notices의 수용 시나리오다 (US4는 007-notifications 뒤에 더한다)
 */
class NoticesAcceptanceTest extends AcceptanceTest {

	private static final String ADMIN_NOTICES = "/api/v1/admin/notices";
	private static final String NOTICES = "/api/v1/notices";
	private static final String NEW = NOTICES + "/new";
	private static final String SEEN = NOTICES + "/seen";
	private static final long MEMBER_ID = 10L;

	@Test
	@DisplayName("US1-AC1 관리자가 게시한 공지는 사용자 목록에서 고정 공지 다음 맨 위에 보인다")
	void publishedNoticeShowsAfterPinned() {
		create("pinned", true, true);
		create("older", false, true);

		MvcTestResult created = asAdmin(post(ADMIN_NOTICES, notice("newest", "<p>body</p>", false, true)));

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");
		assertThat(titles(mvc.get().uri(NOTICES).exchange())).containsExactly("pinned", "newest", "older");
	}

	@Test
	@DisplayName("US1-AC2 임시 저장한 공지는 공지 관리에만 보이고 사용자 목록·상세에는 보이지 않는다")
	void draftIsOnlyInAdmin() {
		long id = create("draft", false, false);

		MvcTestResult admin = asAdmin(mvc.get().uri(ADMIN_NOTICES));
		assertThat(admin).bodyJson().extractingPath("$.content[0].status").isEqualTo("DRAFT");
		assertThat(titles(mvc.get().uri(NOTICES).exchange())).isEmpty();
		assertThat(mvc.get().uri(NOTICES + "/" + id)).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US1-AC3 임시 저장한 공지를 게시하면 그 순간 사용자 목록에 새 공지로 보인다")
	void publishingDraftShowsIt() {
		create("pinned", true, true);
		create("older", false, true);
		long id = create("draft", false, false);

		MvcTestResult published = asAdmin(mvc.post().uri(ADMIN_NOTICES + "/" + id + "/publish"));

		assertThat(published).hasStatus(200);
		assertThat(published).bodyJson().extractingPath("$.publishedAt").isNotNull();
		assertThat(titles(mvc.get().uri(NOTICES).exchange())).containsExactly("pinned", "draft", "older");
		assertThat(asAdmin(mvc.post().uri(ADMIN_NOTICES + "/" + id + "/publish"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US1-AC4 상단에 고정한 공지는 고정하지 않은 공지보다 위에 보이고, 풀면 게시 순서로 돌아간다")
	void pinningMovesToTop() {
		long first = create("first", false, true);
		create("second", false, true);

		assertThat(asAdmin(pin(first, true))).hasStatus(200);
		assertThat(titles(mvc.get().uri(NOTICES).exchange())).containsExactly("first", "second");

		assertThat(asAdmin(pin(first, false))).hasStatus(200);
		assertThat(titles(mvc.get().uri(NOTICES).exchange())).containsExactly("second", "first");
	}

	@Test
	@DisplayName("US1-AC5 게시한 공지를 고치면 사용자에게 고친 내용이 보인다")
	void modifiedNoticeShows() {
		long id = create("before", false, true);

		MvcTestResult modified = asAdmin(mvc.put().uri(ADMIN_NOTICES + "/" + id)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"after\",\"content\":\"<p>changed</p>\"}"));

		assertThat(modified).hasStatus(200);
		MvcTestResult detail = mvc.get().uri(NOTICES + "/" + id).exchange();
		assertThat(detail).bodyJson().extractingPath("$.title").isEqualTo("after");
		assertThat(detail).bodyJson().extractingPath("$.content").isEqualTo("<p>changed</p>");
	}

	@Test
	@DisplayName("US1-AC6 지운 공지는 사용자 목록에서 빠지고 상세는 없는 공지(R001)다")
	void deletedNoticeDisappears() {
		long id = create("gone", false, true);

		assertThat(asAdmin(mvc.delete().uri(ADMIN_NOTICES + "/" + id))).hasStatus(204);

		assertThat(titles(mvc.get().uri(NOTICES).exchange())).isEmpty();
		assertThat(mvc.get().uri(NOTICES + "/" + id)).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US1-AC7 공지 관리 권한이 없는 운영자가 공지를 쓰거나 고치려 하면 권한 없음(A005)으로 막힌다")
	void operatorWithoutPermissionIsForbidden() {
		long id = create("notice", false, true);

		assertThat(asMember(post(ADMIN_NOTICES, notice("try", "<p>x</p>", false, true)))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asMember(mvc.put().uri(ADMIN_NOTICES + "/" + id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"try\",\"content\":\"<p>x</p>\"}"))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US1-AC8 본문의 스크립트처럼 허용하지 않은 내용은 걸러 저장한다")
	void unsafeContentIsSanitized() {
		MvcTestResult created = asAdmin(post(ADMIN_NOTICES, notice("xss",
			"<p>안녕</p><script>alert(1)</script><img src=\\\"x\\\" onerror=\\\"alert(1)\\\">", false, true)));
		long id = id(created);

		String content = JsonPath.read(body(mvc.get().uri(NOTICES + "/" + id).exchange()), "$.content");
		assertThat(content).contains("<p>안녕</p>").doesNotContain("script", "onerror", "<img");
	}

	@Test
	@DisplayName("US2-AC1 사용자 목록은 고정 공지 먼저, 그다음 게시 최신순으로 제목·게시 날짜·고정 여부와 함께 보인다")
	void publicListOrder() {
		create("pinned-old", true, true);
		create("normal-old", false, true);
		create("pinned-new", true, true);
		create("normal-new", false, true);
		create("draft", true, false);

		MvcTestResult list = mvc.get().uri(NOTICES).exchange();

		assertThat(list).hasStatus(200);
		assertThat(titles(list)).containsExactly("pinned-new", "pinned-old", "normal-new", "normal-old");
		assertThat(list).bodyJson().extractingPath("$.content[0].pinned").isEqualTo(true);
		assertThat(list).bodyJson().extractingPath("$.content[2].pinned").isEqualTo(false);
		assertThat(list).bodyJson().extractingPath("$.content[0].publishedAt").isNotNull();
	}

	@Test
	@DisplayName("US2-AC2 공지 상세에 제목, 게시 날짜, 서식과 이미지를 포함한 본문이 보인다")
	void publicDetail() {
		String html = "<h2>head</h2><p><strong>bold</strong></p>"
			+ "<p><img src=\\\"/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c\\\" alt=\\\"\\\"></p>";
		long id = id(asAdmin(post(ADMIN_NOTICES, notice("detail", html, false, true))));

		MvcTestResult detail = mvc.get().uri(NOTICES + "/" + id).exchange();

		assertThat(detail).hasStatus(200);
		assertThat(detail).bodyJson().extractingPath("$.title").isEqualTo("detail");
		assertThat(detail).bodyJson().extractingPath("$.publishedAt").isNotNull();
		String content = JsonPath.read(body(detail), "$.content");
		assertThat(content).contains("<h2>head</h2>", "<strong>bold</strong>",
			"src=\"/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c\"");
	}

	@Test
	@DisplayName("US2-AC3 로그인하지 않아도 로그인한 사용자와 같은 목록과 상세가 보인다")
	void anonymousSeesSame() {
		long id = create("open", false, true);

		MvcTestResult anonymousList = mvc.get().uri(NOTICES).exchange();
		MvcTestResult memberList = asMember(mvc.get().uri(NOTICES));
		MvcTestResult anonymousDetail = mvc.get().uri(NOTICES + "/" + id).exchange();
		MvcTestResult memberDetail = asMember(mvc.get().uri(NOTICES + "/" + id));

		assertThat(anonymousList).hasStatus(200);
		assertThat(anonymousDetail).hasStatus(200);
		assertThat(body(anonymousList)).isEqualTo(body(memberList));
		assertThat(body(anonymousDetail)).isEqualTo(body(memberDetail));
	}

	@Test
	@DisplayName("US3-AC1 공지 목록을 본 뒤 새 공지가 게시되면 새 공지 표시가 있다")
	void newNoticeAfterSeen() {
		assertThat(asMember(mvc.put().uri(SEEN))).hasStatus(204);

		create("fresh", false, true);

		assertThat(asMember(mvc.get().uri(NEW))).hasStatus(200)
			.bodyJson().extractingPath("$.hasNew").isEqualTo(true);
	}

	@Test
	@DisplayName("US3-AC2 새 공지 표시가 있을 때 공지 목록을 열면 표시가 사라진다")
	void seeingClearsNew() {
		create("fresh", false, true);
		assertThat(asMember(mvc.get().uri(NEW))).bodyJson().extractingPath("$.hasNew").isEqualTo(true);

		assertThat(asMember(mvc.put().uri(SEEN))).hasStatus(204);

		assertThat(asMember(mvc.get().uri(NEW))).bodyJson().extractingPath("$.hasNew").isEqualTo(false);
	}

	@Test
	@DisplayName("US3-AC3 공지 목록을 본 뒤 임시 저장만 됐으면 새 공지 표시가 없다")
	void draftDoesNotMakeNew() {
		assertThat(asMember(mvc.put().uri(SEEN))).hasStatus(204);

		create("draft", false, false);

		assertThat(asMember(mvc.get().uri(NEW))).bodyJson().extractingPath("$.hasNew").isEqualTo(false);
	}

	private long create(String title, boolean pinned, boolean publish) {
		MvcTestResult result = asAdmin(post(ADMIN_NOTICES, notice(title, "<p>" + title + "</p>", pinned, publish)));
		assertThat(result).hasStatus(201);
		return id(result);
	}

	private MockMvcRequestBuilder post(String uri, String json) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MockMvcRequestBuilder pin(long id, boolean pinned) {
		return mvc.put().uri(ADMIN_NOTICES + "/" + id + "/pin").contentType(MediaType.APPLICATION_JSON)
			.content("{\"pinned\":" + pinned + "}");
	}

	private static String notice(String title, String content, boolean pinned, boolean publish) {
		return "{\"title\":\"" + title + "\",\"content\":\"" + content + "\",\"pinned\":" + pinned
			+ ",\"publish\":" + publish + "}";
	}

	private List<String> titles(MvcTestResult result) {
		return JsonPath.read(body(result), "$.content[*].title");
	}

	private long id(MvcTestResult result) {
		Number id = JsonPath.read(body(result), "$.id");
		return id.longValue();
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	/** 역할 없는 일반 회원 (공지 관리 권한이 없는 운영자와 같다. 운영자는 시스템 역할이 USER다) */
	private MvcTestResult asMember(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID, "USER")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
