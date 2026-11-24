package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.support.AcceptanceTest;

/**
 * 배너·팝업 관리. 스펙 specs/016-banner-popup의 수용 시나리오다.
 * 화면에서만 보이는 동작(US1-AC10 미리보기, 처리 뒤 고른 것이 풀림, 칸 아래 이유)은 e2e(banners·popups)가 본다 (research R11).
 * 상태는 시작·끝 일시를 과거·미래로 넣어 만든다. 시계를 바꾸지 않는다.
 */
@TestPropertySource(properties = {"app.rich-text.image-dir=build/tmp/rich-text-images",
	"app.rich-text.image-max-size=1KB"})
class BannerPopupAcceptanceTest extends AcceptanceTest {

	private static final String BANNERS = "/api/v1/admin/banners";
	private static final String POPUPS = "/api/v1/admin/popups";
	private static final String IMAGES = "/api/v1/rich-text/images";
	private static final byte[] PNG = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D',
		'R'};
	private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0, 0, 0};
	private static final OffsetDateTime NOW = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// ---- US1 배너 ----

	@Test
	@DisplayName("US1-AC1 배너를 등록하면 목록에 이미지와 함께 보이고, 시작이 지났으면 노출 중, 아니면 예정이다")
	void createdBannerShowsWithStatus() {
		String image = upload(PNG);

		MvcTestResult created = asAdmin(post(BANNERS, banner("지난 시작", image, null, NOW.minusDays(1), null, 0, true)));
		asAdmin(post(BANNERS, banner("내일 시작", upload(PNG), null, NOW.plusDays(1), null, 0, true)));

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.wideImageUrl").isEqualTo(image);
		MvcTestResult list = asAdmin(mvc.get().uri(BANNERS));
		assertThat(statusOf(list, "지난 시작")).isEqualTo("ACTIVE");
		assertThat(statusOf(list, "내일 시작")).isEqualTo("SCHEDULED");
		assertThat(list).bodyJson().extractingPath("$.content[?(@.name == '지난 시작')].wideImageUrl").asArray()
			.containsExactly(image);
	}

	@Test
	@DisplayName("US1-AC2 끝 일시가 지난 배너는 끝남이고, 기본 목록에서는 빠지며 전체로 보면 보인다")
	void endedBannerIsHiddenByDefault() {
		createBanner("끝난 배너", NOW.minusDays(2), NOW.minusDays(1), 0, true);
		createBanner("노출 중", NOW.minusDays(1), null, 0, true);

		assertThat(names(asAdmin(mvc.get().uri(BANNERS)))).containsExactly("노출 중");
		MvcTestResult all = asAdmin(mvc.get().uri(BANNERS).param("status", "ALL"));
		assertThat(statusOf(all, "끝난 배너")).isEqualTo("ENDED");
		assertThat(names(asAdmin(mvc.get().uri(BANNERS).param("status", "ENDED")))).containsExactly("끝난 배너");
	}

	@Test
	@DisplayName("US1-AC3 노출 중인 배너를 끄면 꺼짐이 되고, 다시 켜면 기간에 맞는 상태로 돌아간다")
	void disablingAndEnablingBanner() {
		String image = upload(PNG);
		long id = createBanner("배너", image, NOW.minusDays(1), null, 0, true);

		MvcTestResult off = asAdmin(
			put(BANNERS + "/" + id, banner("배너", image, null, NOW.minusDays(1), null, 0, false)));
		assertThat(off).hasStatus(200);
		assertThat(off).bodyJson().extractingPath("$.status").isEqualTo("DISABLED");

		MvcTestResult on = asAdmin(put(BANNERS + "/" + id, banner("배너", image, null, NOW.minusDays(1), null, 0, true)));
		assertThat(on).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
	}

	@Test
	@DisplayName("US1-AC4 순서 숫자를 바꾸면 목록이 작은 것부터, 같으면 노출 시작이 늦은 것부터 보인다")
	void bannersFollowSortOrder() {
		createBanner("A", NOW.minusDays(3), null, 0, true);
		createBanner("B", NOW.minusDays(1), null, 0, true);
		String image = upload(PNG);
		long idC = createBanner("C", image, NOW.minusDays(2), null, 5, true);
		assertThat(names(asAdmin(mvc.get().uri(BANNERS)))).containsExactly("B", "A", "C");

		assertThat(asAdmin(put(BANNERS + "/" + idC, banner("C", image, null, NOW.minusDays(2), null, 0, true))))
			.hasStatus(200);

		assertThat(names(asAdmin(mvc.get().uri(BANNERS)))).containsExactly("B", "C", "A");
	}

	@Test
	@DisplayName("US1-AC5 지운 배너는 목록에서 빠지고 상세는 없는 배너(R001)다")
	void deletedBannerDisappears() {
		long id = createBanner("지울 배너", NOW.minusDays(1), null, 0, true);

		assertThat(asAdmin(mvc.delete().uri(BANNERS + "/" + id))).hasStatus(204);

		assertThat(names(asAdmin(mvc.get().uri(BANNERS).param("status", "ALL")))).isEmpty();
		assertThat(asAdmin(mvc.get().uri(BANNERS + "/" + id))).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US1-AC6 배너 관리 권한이 없는 운영자는 배너를 등록하거나 고칠 수 없다(A005)")
	void operatorWithoutPermissionIsBlocked() {
		long id = createBanner("배너", NOW.minusDays(1), null, 0, true);
		String popupOnly = operator(30L, "popup-op", "POPUP_MANAGE");
		String json = banner("새 배너", upload(PNG), null, NOW, null, 0, true);

		assertThat(post(BANNERS, json).header(HttpHeaders.AUTHORIZATION, popupOnly).exchange()).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(put(BANNERS + "/" + id, json).header(HttpHeaders.AUTHORIZATION, popupOnly).exchange())
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US1-AC7 끝 일시가 시작보다 앞이거나 허용하지 않은 연결 주소면 저장되지 않고 그 칸에 이유가 온다(V001)")
	void invalidPeriodOrLinkIsRejected() {
		String image = upload(PNG);

		MvcTestResult period = asAdmin(post(BANNERS, banner("배너", image, null, NOW, NOW.minusHours(1), 0, true)));
		assertThat(period).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(period).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("endsAt");
		assertThat(asAdmin(post(BANNERS, banner("배너", image, null, NOW, NOW, 0, true)))).hasStatus(400);

		for (String link : List.of("javascript:alert(1)", "//evil.example", "ftp://x")) {
			MvcTestResult result = asAdmin(post(BANNERS, banner("배너", image, link, NOW, null, 0, true)));
			assertThat(result).hasStatus(400);
			assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("linkUrl");
		}
		assertThat(names(asAdmin(mvc.get().uri(BANNERS).param("status", "ALL")))).isEmpty();
	}

	@Test
	@DisplayName("US1-AC8 GIF 이미지로는 배너를 저장할 수 없고, 크기 제한을 넘는 파일은 올라가지 않는다(V001)")
	void disallowedImageIsRejected() {
		MvcTestResult gif = asAdmin(post(BANNERS, banner("배너", upload(GIF), null, NOW, null, 0, true)));
		assertThat(gif).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(gif).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("wideImageUrl");

		byte[] large = new byte[2048];
		System.arraycopy(PNG, 0, large, 0, PNG.length);
		assertThat(uploadResult(large)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");

		MvcTestResult missing = asAdmin(
			post(BANNERS, banner("배너", IMAGES + "/00000000-0000-0000-0000-000000000000", null, NOW, null, 0, true)));
		assertThat(missing).hasStatus(400).bodyJson().extractingPath("$.errors[*].field").asArray()
			.containsExactly("wideImageUrl");
	}

	@Test
	@DisplayName("US1-AC9 넓은 화면 이미지를 바꿔 저장하면 상세에 새 이미지가 보이고, 이전 이미지는 정리 대상(임시)이 된다")
	void replacingImageReleasesOldOne() {
		String before = upload(PNG);
		long id = createBanner("배너", before, NOW.minusDays(1), null, 0, true);
		assertThat(imageStatus(before)).isEqualTo("USED");
		String after = upload(PNG);

		assertThat(asAdmin(put(BANNERS + "/" + id, banner("배너", after, null, NOW.minusDays(1), null, 0, true))))
			.hasStatus(200);

		assertThat(asAdmin(mvc.get().uri(BANNERS + "/" + id))).bodyJson().extractingPath("$.wideImageUrl")
			.isEqualTo(after);
		assertThat(imageStatus(after)).isEqualTo("USED");
		assertThat(imageStatus(before)).isEqualTo("TEMP");
	}

	@Test
	@DisplayName("US1-AC11 셋 중 둘을 한꺼번에 지우면 둘이 빠지고 하나는 남는다. 없는 배너가 섞이면 아무것도 지우지 않는다(R001)")
	void bulkDeleteBanners() {
		long idA = createBanner("A", NOW.minusDays(1), null, 0, true);
		long idB = createBanner("B", NOW.minusDays(2), null, 0, true);
		createBanner("C", NOW.minusDays(3), null, 0, true);

		assertThat(asAdmin(post(BANNERS + "/bulk-delete", "{\"ids\":[" + idA + ",999999]}"))).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(names(asAdmin(mvc.get().uri(BANNERS)))).containsExactly("A", "B", "C");

		assertThat(asAdmin(post(BANNERS + "/bulk-delete", "{\"ids\":[" + idA + "," + idB + "]}"))).hasStatus(204);
		assertThat(names(asAdmin(mvc.get().uri(BANNERS)))).containsExactly("C");
	}

	@Test
	@DisplayName("US1-AC12 노출 중·꺼짐 배너 둘을 한꺼번에 끄면 둘 다 꺼짐, 다시 켜면 기간에 맞는 상태다")
	void bulkEnableBanners() {
		long active = createBanner("노출 중", NOW.minusDays(1), null, 0, true);
		long off = createBanner("꺼짐", NOW.minusDays(2), null, 0, false);
		String ids = "[" + active + "," + off + "]";

		assertThat(asAdmin(put(BANNERS + "/enabled", "{\"ids\":" + ids + ",\"enabled\":false}"))).hasStatus(204);
		MvcTestResult disabled = asAdmin(mvc.get().uri(BANNERS));
		assertThat(statusOf(disabled, "노출 중")).isEqualTo("DISABLED");
		assertThat(statusOf(disabled, "꺼짐")).isEqualTo("DISABLED");

		assertThat(asAdmin(put(BANNERS + "/enabled", "{\"ids\":" + ids + ",\"enabled\":true}"))).hasStatus(204);
		MvcTestResult enabled = asAdmin(mvc.get().uri(BANNERS));
		assertThat(statusOf(enabled, "노출 중")).isEqualTo("ACTIVE");
		assertThat(statusOf(enabled, "꺼짐")).isEqualTo("ACTIVE");
	}

	@Test
	@DisplayName("US1-AC13 이름 글자로 찾으면 그 이름의 배너만 보이고, 상태 거르기와 함께 쓸 수 있다")
	void searchBannersByName() {
		createBanner("윈터컵", NOW.minusDays(1), null, 0, true);
		createBanner("점검", NOW.minusDays(1), null, 0, true);
		createBanner("윈터 예고", NOW.plusDays(1), null, 0, true);

		assertThat(names(asAdmin(mvc.get().uri(BANNERS).param("keyword", "윈터")))).containsExactlyInAnyOrder("윈터컵",
			"윈터 예고");
		assertThat(names(asAdmin(mvc.get().uri(BANNERS).param("keyword", "윈터").param("status", "ACTIVE"))))
			.containsExactly("윈터컵");
	}

	@Test
	@DisplayName("US1-AC14 순서 0·켜짐으로 등록하면 그대로 저장된다 (화면 기본값은 e2e)")
	void defaultsAreSaved() {
		MvcTestResult created = asAdmin(post(BANNERS, banner("배너", upload(PNG), null, NOW, null, 0, true)));

		assertThat(created).bodyJson().extractingPath("$.sortOrder").isEqualTo(0);
		assertThat(created).bodyJson().extractingPath("$.enabled").isEqualTo(true);
	}

	// ---- US2 팝업 ----

	@Test
	@DisplayName("US2-AC1 팝업을 등록하면 목록에 보이고 상태는 배너와 같은 규칙이다")
	void createdPopupShowsWithStatus() {
		MvcTestResult created = asAdmin(post(POPUPS, popup("지난 시작", "<p>안내</p>", NOW.minusDays(1), null, 0, true)));
		asAdmin(post(POPUPS, popup("내일 시작", "<p>안내</p>", NOW.plusDays(1), null, 0, true)));

		assertThat(created).hasStatus(201);
		MvcTestResult list = asAdmin(mvc.get().uri(POPUPS));
		assertThat(popupStatusOf(list, "지난 시작")).isEqualTo("ACTIVE");
		assertThat(popupStatusOf(list, "내일 시작")).isEqualTo("SCHEDULED");
	}

	@Test
	@DisplayName("US2-AC2 팝업을 끄면 꺼짐이 되고, 지우면 목록에서 빠진다")
	void disablingAndDeletingPopup() {
		long id = createPopup("팝업", NOW.minusDays(1), 0, true);

		MvcTestResult off = asAdmin(put(POPUPS + "/" + id, popup("팝업", "<p>안내</p>", NOW.minusDays(1), null, 0, false)));
		assertThat(off).bodyJson().extractingPath("$.status").isEqualTo("DISABLED");

		assertThat(asAdmin(mvc.delete().uri(POPUPS + "/" + id))).hasStatus(204);
		assertThat(titles(asAdmin(mvc.get().uri(POPUPS).param("status", "ALL")))).isEmpty();
	}

	@Test
	@DisplayName("US2-AC3 팝업 관리 권한이 없는 운영자는 팝업을 등록하거나 고칠 수 없다(A005)")
	void popupOperatorWithoutPermissionIsBlocked() {
		long id = createPopup("팝업", NOW.minusDays(1), 0, true);
		String bannerOnly = operator(31L, "banner-op", "BANNER_MANAGE");
		String json = popup("새 팝업", "<p>안내</p>", NOW, null, 0, true);

		assertThat(post(POPUPS, json).header(HttpHeaders.AUTHORIZATION, bannerOnly).exchange()).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(put(POPUPS + "/" + id, json).header(HttpHeaders.AUTHORIZATION, bannerOnly).exchange())
			.hasStatus(403);
	}

	@Test
	@DisplayName("US2-AC4 끝 일시가 시작보다 앞이거나 허용하지 않은 연결 주소면 팝업이 저장되지 않는다(V001)")
	void invalidPopupIsRejected() {
		MvcTestResult period = asAdmin(post(POPUPS, popup("팝업", "<p>안내</p>", NOW, NOW.minusHours(1), 0, true)));
		assertThat(period).hasStatus(400).bodyJson().extractingPath("$.errors[*].field").asArray()
			.containsExactly("endsAt");

		String withLink = "{\"title\":\"팝업\",\"content\":\"<p>안내</p>\",\"linkUrl\":\"javascript:alert(1)\","
			+ "\"startsAt\":\"" + NOW + "\",\"sortOrder\":0,\"enabled\":true}";
		assertThat(asAdmin(post(POPUPS, withLink))).hasStatus(400).bodyJson().extractingPath("$.errors[*].field")
			.asArray().containsExactly("linkUrl");
	}

	@Test
	@DisplayName("US2-AC5 팝업 셋 중 둘을 한꺼번에 지우면 둘이 빠지고 하나는 남는다")
	void bulkDeletePopups() {
		long idA = createPopup("A", NOW.minusDays(1), 0, true);
		long idB = createPopup("B", NOW.minusDays(2), 0, true);
		createPopup("C", NOW.minusDays(3), 0, true);

		assertThat(asAdmin(post(POPUPS + "/bulk-delete", "{\"ids\":[" + idA + "," + idB + "]}"))).hasStatus(204);

		assertThat(titles(asAdmin(mvc.get().uri(POPUPS)))).containsExactly("C");
	}

	@Test
	@DisplayName("US2-AC6 켜진 팝업 둘을 한꺼번에 끄면 둘 다 꺼짐이다")
	void bulkDisablePopups() {
		long idA = createPopup("A", NOW.minusDays(1), 0, true);
		long idB = createPopup("B", NOW.plusDays(1), 0, true);

		assertThat(asAdmin(put(POPUPS + "/enabled", "{\"ids\":[" + idA + "," + idB + "],\"enabled\":false}")))
			.hasStatus(204);

		MvcTestResult list = asAdmin(mvc.get().uri(POPUPS));
		assertThat(popupStatusOf(list, "A")).isEqualTo("DISABLED");
		assertThat(popupStatusOf(list, "B")).isEqualTo("DISABLED");
	}

	@Test
	@DisplayName("US2-AC7 팝업 내용의 스크립트 같은 허용하지 않은 내용은 걸러져 저장된다")
	void popupContentIsSanitized() {
		MvcTestResult created = asAdmin(
			post(POPUPS, popup("팝업", "<p>안녕</p><script>alert(1)</script>", NOW, null, 0, true)));

		assertThat(created).hasStatus(201);
		long id = id(created);
		String content = JsonPath.read(body(asAdmin(mvc.get().uri(POPUPS + "/" + id))), "$.content");
		assertThat(content).contains("<p>안녕</p>").doesNotContain("script");
	}

	@Test
	@DisplayName("US2-AC8 팝업 순서 숫자를 바꾸면 목록이 배너와 같은 순서 규칙으로 보인다")
	void popupsFollowSortOrder() {
		createPopup("A", NOW.minusDays(3), 0, true);
		createPopup("B", NOW.minusDays(1), 0, true);
		long idC = createPopup("C", NOW.minusDays(2), 5, true);
		assertThat(titles(asAdmin(mvc.get().uri(POPUPS)))).containsExactly("B", "A", "C");

		asAdmin(put(POPUPS + "/" + idC, popup("C", "<p>안내</p>", NOW.minusDays(2), null, 0, true)));

		assertThat(titles(asAdmin(mvc.get().uri(POPUPS)))).containsExactly("B", "C", "A");
	}

	// ---- 도우미 ----

	private long createBanner(String name, OffsetDateTime startsAt, @Nullable OffsetDateTime endsAt, int sortOrder,
		boolean enabled) {
		return createBanner(name, upload(PNG), startsAt, endsAt, sortOrder, enabled);
	}

	private long createBanner(String name, String image, OffsetDateTime startsAt, @Nullable OffsetDateTime endsAt,
		int sortOrder, boolean enabled) {
		return id(asAdmin(post(BANNERS, banner(name, image, null, startsAt, endsAt, sortOrder, enabled))));
	}

	private long createPopup(String title, OffsetDateTime startsAt, int sortOrder, boolean enabled) {
		return id(asAdmin(post(POPUPS, popup(title, "<p>안내</p>", startsAt, null, sortOrder, enabled))));
	}

	private static String banner(String name, String image, @Nullable String linkUrl, OffsetDateTime startsAt,
		@Nullable OffsetDateTime endsAt, int sortOrder, boolean enabled) {
		return "{\"name\":\"" + name + "\",\"wideImageUrl\":\"" + image + "\",\"altText\":\"" + name + " 안내\""
			+ (linkUrl == null ? "" : ",\"linkUrl\":\"" + linkUrl + "\"") + ",\"startsAt\":\"" + startsAt + "\""
			+ (endsAt == null ? "" : ",\"endsAt\":\"" + endsAt + "\"") + ",\"sortOrder\":" + sortOrder
			+ ",\"enabled\":" + enabled + "}";
	}

	private static String popup(String title, String content, OffsetDateTime startsAt,
		@Nullable OffsetDateTime endsAt, int sortOrder, boolean enabled) {
		return "{\"title\":\"" + title + "\",\"content\":\"" + content + "\",\"startsAt\":\"" + startsAt + "\""
			+ (endsAt == null ? "" : ",\"endsAt\":\"" + endsAt + "\"") + ",\"sortOrder\":" + sortOrder
			+ ",\"enabled\":" + enabled + "}";
	}

	/** rich-text 이미지 올리기로 올리고 받은 주소 */
	private String upload(byte[] content) {
		MvcTestResult result = uploadResult(content);
		assertThat(result).hasStatus(201);
		return JsonPath.read(body(result), "$.url");
	}

	private MvcTestResult uploadResult(byte[] content) {
		return mvc.post().uri(IMAGES).multipart()
			.file(new MockMultipartFile("file", "banner.bin", "application/octet-stream", content))
			.header(HttpHeaders.AUTHORIZATION, superAdmin())
			.exchange();
	}

	private String imageStatus(String url) {
		return jdbcTemplate.queryForObject("SELECT status FROM tb_rich_text_image WHERE public_id = ?", String.class,
			url.substring(url.lastIndexOf('/') + 1));
	}

	private MockMvcRequestBuilder post(String uri, String json) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MockMvcRequestBuilder put(String uri, String json) {
		return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();
	}

	private List<String> names(MvcTestResult result) {
		return JsonPath.read(body(result), "$.content[*].name");
	}

	private List<String> titles(MvcTestResult result) {
		return JsonPath.read(body(result), "$.content[*].title");
	}

	private String statusOf(MvcTestResult result, String name) {
		List<String> statuses = JsonPath.read(body(result), "$.content[?(@.name == '" + name + "')].status");
		assertThat(statuses).hasSize(1);
		return statuses.getFirst();
	}

	private String popupStatusOf(MvcTestResult result, String title) {
		List<String> statuses = JsonPath.read(body(result), "$.content[?(@.title == '" + title + "')].status");
		assertThat(statuses).hasSize(1);
		return statuses.getFirst();
	}

	private long id(MvcTestResult result) {
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
