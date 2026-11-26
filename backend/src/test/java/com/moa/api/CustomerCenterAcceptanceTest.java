package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.TestUsers;

/**
 * 고객센터(FAQ·1:1 문의). 스펙 specs/017-customer-center의 수용 시나리오다.
 * 화면에서만 보이는 동작(US2-AC3 펼치기, US3-AC2 로그인 뒤 돌아오기, US3-AC3 안내, US4-AC2 드로어)은 e2e(support)가 본다 (research R13).
 * US5(탈퇴 회원 문의)는 회원 탈퇴 스펙 뒤에 더한다.
 */
@TestPropertySource(properties = {"app.rich-text.image-dir=build/tmp/rich-text-images"})
class CustomerCenterAcceptanceTest extends AcceptanceTest {

	private static final String FAQS = "/api/v1/faqs";
	private static final String ADMIN_FAQS = "/api/v1/admin/faqs";
	private static final String INQUIRIES = "/api/v1/inquiries";
	private static final String ADMIN_INQUIRIES = "/api/v1/admin/inquiries";
	private static final String IMAGES = "/api/v1/rich-text/images";
	private static final byte[] PNG = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D',
		'R'};
	private static final long MEMBER_A = 21L;
	private static final long MEMBER_B = 22L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** DatabaseCleaner가 분류 코드(SUPPORT_CATEGORY)까지 지우므로 처음 구성으로 되돌린다 */
	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
		testUsers.create(MEMBER_A, "member-a", "member1234!", Role.USER);
		testUsers.create(MEMBER_B, "member-b", "member1234!", Role.USER);
	}

	// ---- US1 FAQ 관리 ----

	@Test
	@DisplayName("US1-AC1 FAQ를 등록하면 관리 목록에 켜진 채 보이고, 사용자 FAQ의 그 분류에 보인다")
	void createdFaqShowsEverywhere() {
		MvcTestResult created = asAdmin(post(ADMIN_FAQS, faq("ACCOUNT", "비밀번호를 잊었어요", "<p>찾기에서 바꿔요</p>", 0, true)));

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.categoryName").isEqualTo("계정");
		MvcTestResult list = asAdmin(mvc.get().uri(ADMIN_FAQS));
		assertThat(list).bodyJson().extractingPath("$[*].question").asArray().containsExactly("비밀번호를 잊었어요");
		assertThat(list).bodyJson().extractingPath("$[0].enabled").isEqualTo(true);
		assertThat(questions(mvc.get().uri(FAQS).param("categoryCode", "ACCOUNT").exchange()))
			.containsExactly("비밀번호를 잊었어요");
	}

	@Test
	@DisplayName("US1-AC2 켜진 FAQ를 끄면 관리 목록에는 꺼짐으로 남고 사용자 FAQ에서는 빠진다")
	void disabledFaqIsHiddenFromUsers() {
		long id = createFaq("ACCOUNT", "끌 질문", 0, true);

		assertThat(asAdmin(put(ADMIN_FAQS + "/" + id, faq("ACCOUNT", "끌 질문", "<p>답</p>", 0, false))))
			.hasStatus(200);

		assertThat(asAdmin(mvc.get().uri(ADMIN_FAQS))).bodyJson().extractingPath("$[0].enabled").isEqualTo(false);
		assertThat(questions(mvc.get().uri(FAQS).exchange())).isEmpty();
	}

	@Test
	@DisplayName("US1-AC3 순서 숫자를 다른 FAQ보다 작게 바꾸면 관리 목록과 사용자 FAQ에서 맨 앞에 보인다")
	void sortOrderMovesFaqFirst() {
		createFaq("ACCOUNT", "첫째", 10, true);
		createFaq("ACCOUNT", "둘째", 10, true);
		long third = createFaq("ACCOUNT", "셋째", 10, true);

		asAdmin(put(ADMIN_FAQS + "/" + third, faq("ACCOUNT", "셋째", "<p>답</p>", 1, true)));

		assertThat(adminQuestions()).containsExactly("셋째", "둘째", "첫째");
		assertThat(questions(mvc.get().uri(FAQS).exchange())).containsExactly("셋째", "둘째", "첫째");
	}

	@Test
	@DisplayName("US1-AC4 FAQ의 질문이나 답을 고치면 사용자 FAQ에 고친 내용이 보인다")
	void modifiedFaqIsShown() {
		long id = createFaq("USAGE", "예전 질문", 0, true);

		asAdmin(put(ADMIN_FAQS + "/" + id, faq("USAGE", "새 질문", "<p>새 답</p>", 0, true)));

		MvcTestResult list = mvc.get().uri(FAQS).exchange();
		assertThat(list).bodyJson().extractingPath("$[0].question").isEqualTo("새 질문");
		assertThat(list).bodyJson().extractingPath("$[0].answer").asString().contains("새 답");
	}

	@Test
	@DisplayName("US1-AC5 켜진 하나와 꺼진 하나를 한꺼번에 지우면 둘 다 빠지고 사용자 FAQ에는 남은 하나만 보인다")
	void bulkDeleteFaqs() {
		long on = createFaq("ACCOUNT", "지울 켜짐", 0, true);
		createFaq("ACCOUNT", "남길 켜짐", 0, true);
		long off = createFaq("ACCOUNT", "지울 꺼짐", 0, false);

		assertThat(asAdmin(post(ADMIN_FAQS + "/bulk-delete", "{\"ids\":[" + on + "," + off + "]}"))).hasStatus(204);

		assertThat(adminQuestions()).containsExactly("남길 켜짐");
		assertThat(questions(mvc.get().uri(FAQS).exchange())).containsExactly("남길 켜짐");
	}

	@Test
	@DisplayName("US1-AC5 한꺼번에 지울 때 없는 FAQ가 섞이면 R001이고 아무것도 지우지 않는다")
	void bulkDeleteWithMissingFaqChangesNothing() {
		long id = createFaq("ACCOUNT", "남을 질문", 0, true);

		assertThat(asAdmin(post(ADMIN_FAQS + "/bulk-delete", "{\"ids\":[" + id + ",999999]}"))).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");

		assertThat(adminQuestions()).containsExactly("남을 질문");
	}

	@Test
	@DisplayName("US1-AC6 꺼진 FAQ 둘을 한꺼번에 켜면 둘 다 사용자 FAQ에 보인다")
	void bulkEnableFaqs() {
		long first = createFaq("ACCOUNT", "켤 첫째", 0, false);
		long second = createFaq("ACCOUNT", "켤 둘째", 0, false);

		assertThat(asAdmin(put(ADMIN_FAQS + "/enabled", "{\"ids\":[" + first + "," + second + "],\"enabled\":true}")))
			.hasStatus(204);

		assertThat(questions(mvc.get().uri(FAQS).exchange())).containsExactlyInAnyOrder("켤 첫째", "켤 둘째");
	}

	@Test
	@DisplayName("US1-AC7 FAQ 관리 권한이 없는 운영자는 FAQ를 등록하거나 고칠 수 없다(A005)")
	void operatorWithoutFaqPermissionIsForbidden() {
		long id = createFaq("ACCOUNT", "남의 질문", 0, true);
		String inquiryOnly = operator(30L, "inquiry-op", "INQUIRY_MANAGE");

		assertThat(post(ADMIN_FAQS, faq("ACCOUNT", "질문", "<p>답</p>", 0, true))
			.header(HttpHeaders.AUTHORIZATION, inquiryOnly).exchange()).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(put(ADMIN_FAQS + "/" + id, faq("ACCOUNT", "바꾼 질문", "<p>답</p>", 0, true))
			.header(HttpHeaders.AUTHORIZATION, inquiryOnly).exchange()).hasStatus(403);
	}

	@Test
	@DisplayName("US1-AC8 답에 넣은 스크립트는 걸러져 저장되고 사용자 FAQ에 나가지 않는다")
	void scriptInAnswerIsRemoved() {
		asAdmin(post(ADMIN_FAQS, faq("ACCOUNT", "스크립트", "<p>안녕</p><script>alert(1)</script>", 0, true)));

		MvcTestResult list = mvc.get().uri(FAQS).exchange();
		assertThat(list).bodyJson().extractingPath("$[0].answer").asString().contains("안녕").doesNotContain("script");
	}

	@Test
	@DisplayName("FR-015 지우지 않은 FAQ가 200개면 등록을 B301로 막는다")
	void faqLimit() {
		jdbcTemplate.batchUpdate(
			"insert into tb_faq (category_code, question, answer, sort_order, enabled, created_at, updated_at, "
				+ "created_by, updated_by) values ('ACCOUNT', ?, '<p>답</p>', 0, true, now(6), now(6), 1, 1)",
			Collections.nCopies(200, new Object[] {"채운 질문"}));

		assertThat(asAdmin(post(ADMIN_FAQS, faq("ACCOUNT", "201번째", "<p>답</p>", 0, true)))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B301");
	}

	@Test
	@DisplayName("FR-003 사용 안 함 분류로는 새 FAQ를 쓸 수 없지만, 그 분류의 FAQ는 분류를 그대로 두면 고칠 수 있다")
	void inactiveCategory() {
		long id = createFaq("ETC", "기타 질문", 0, true);
		jdbcTemplate
			.update("update tb_code set enabled = false where group_code = 'SUPPORT_CATEGORY' and code = 'ETC'");

		assertThat(asAdmin(post(ADMIN_FAQS, faq("ETC", "새 기타", "<p>답</p>", 0, true)))).hasStatus(400)
			.bodyJson().extractingPath("$.fieldErrors[0].field").isEqualTo("categoryCode");
		assertThat(asAdmin(put(ADMIN_FAQS + "/" + id, faq("ETC", "고친 기타", "<p>답</p>", 0, true)))).hasStatus(200)
			.bodyJson().extractingPath("$.categoryName").isEqualTo("기타");
		assertThat(questions(mvc.get().uri(FAQS).exchange())).isEmpty();
		assertThat(mvc.get().uri(FAQS + "/categories").exchange()).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("ACCOUNT", "USAGE", "ERROR", "SUGGESTION");
	}

	// ---- US2 사용자 FAQ ----

	@Test
	@DisplayName("US2-AC1 로그인 없이 고객센터를 열면 켜진 FAQ가 분류와 상관없이 순서대로 보인다")
	void publicFaqListShowsAll() {
		createFaq("ACCOUNT", "계정 질문", 1, true);
		createFaq("USAGE", "이용 질문", 0, true);

		MvcTestResult list = mvc.get().uri(FAQS).exchange();

		assertThat(list).hasStatus(200);
		assertThat(questions(list)).containsExactly("이용 질문", "계정 질문");
		assertThat(list).bodyJson().extractingPath("$[0].categoryName").isEqualTo("이용 방법");
	}

	@Test
	@DisplayName("US2-AC2 분류를 고르면 그 분류의 켜진 FAQ만 보인다")
	void publicFaqListByCategory() {
		createFaq("ACCOUNT", "계정 질문", 0, true);
		createFaq("USAGE", "이용 질문", 0, true);

		assertThat(questions(mvc.get().uri(FAQS).param("categoryCode", "USAGE").exchange()))
			.containsExactly("이용 질문");
	}

	@Test
	@DisplayName("US2-AC4 검색어가 질문이나 답에 든 FAQ만 보이고, 없으면 빈 목록이다")
	void publicFaqSearch() {
		createFaq("ACCOUNT", "비밀번호를 잊었어요", 0, true);
		asAdmin(post(ADMIN_FAQS, faq("ACCOUNT", "로그인이 안 돼요", "<p>비밀번호를 확인해요</p>", 0, true)));
		createFaq("USAGE", "파티는 어떻게 만들어요", 0, true);

		assertThat(questions(mvc.get().uri(FAQS).param("keyword", "비밀번호").exchange()))
			.containsExactlyInAnyOrder("비밀번호를 잊었어요", "로그인이 안 돼요");
		assertThat(questions(mvc.get().uri(FAQS).param("keyword", "비밀번호").param("categoryCode", "USAGE")
			.exchange())).isEmpty();
	}

	@Test
	@DisplayName("US2-AC5 로그인하지 않아도 로그인한 사용자와 같은 FAQ와 분류가 보인다")
	void publicFaqForAnonymous() {
		createFaq("ACCOUNT", "공개 질문", 0, true);

		assertThat(questions(mvc.get().uri(FAQS).exchange()))
			.isEqualTo(questions(mvc.get().uri(FAQS).header(HttpHeaders.AUTHORIZATION, member(MEMBER_A)).exchange()));
		assertThat(mvc.get().uri(FAQS + "/categories").exchange()).hasStatus(200).bodyJson()
			.extractingPath("$[*].name").asArray().containsExactly("계정", "이용 방법", "오류", "제안", "기타");
	}

	// ---- US3 회원 문의 ----

	@Test
	@DisplayName("US3-AC1 이미지 두 장을 붙여 문의를 남기면 내 문의 맨 위에 답변 대기로 보이고 상세에 이미지가 있다")
	void memberCreatesInquiry() {
		String first = upload(MEMBER_A);
		String second = upload(MEMBER_A);
		createInquiry(MEMBER_A, "예전 문의", List.of());

		MvcTestResult created = asMember(MEMBER_A,
			post(INQUIRIES, inquiry("ACCOUNT", "로그인이 안 돼요", "어제부터\n안 돼요", List.of(first, second))));

		assertThat(created).hasStatus(201);
		long id = id(created);
		MvcTestResult mine = asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/me"));
		assertThat(mine).bodyJson().extractingPath("$.content[0].title").isEqualTo("로그인이 안 돼요");
		assertThat(mine).bodyJson().extractingPath("$.content[0].status").isEqualTo("WAITING");
		MvcTestResult detail = asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/" + id));
		assertThat(detail).bodyJson().extractingPath("$.imageUrls").asArray().containsExactly(first, second);
		assertThat(detail).bodyJson().extractingPath("$.content").isEqualTo("어제부터\n안 돼요");
		assertThat(detail).bodyJson().extractingPath("$.categoryName").isEqualTo("계정");
	}

	@Test
	@DisplayName("US3-AC3 이미지를 여섯 장 붙이면 V001로 막힌다")
	void sixImagesAreRejected() {
		List<String> six = List.of(upload(MEMBER_A), upload(MEMBER_A), upload(MEMBER_A), upload(MEMBER_A),
			upload(MEMBER_A), upload(MEMBER_A));

		assertThat(asMember(MEMBER_A, post(INQUIRIES, inquiry("ACCOUNT", "여섯 장", "내용", six)))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("US3-AC4 답변이 달린 문의 상세에는 답변과 답변 시각이 보이고 답변 완료다. 답변한 관리자는 보이지 않는다")
	void memberSeesAnswer() {
		long id = createInquiry(MEMBER_A, "답변 받을 문의", List.of());
		asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>이렇게 해 보세요</p>\"}"));

		MvcTestResult detail = asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/" + id));

		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo("ANSWERED");
		assertThat(detail).bodyJson().extractingPath("$.answer").asString().contains("이렇게 해 보세요");
		assertThat(detail).bodyJson().extractingPath("$.answeredAt").isNotNull();
		assertThat(detail).bodyJson().doesNotHavePath("$.answeredByName");
	}

	@Test
	@DisplayName("US3-AC5 답변 대기 문의를 지우면 내 문의와 문의 관리 목록에서 빠지고 첨부 이미지는 임시로 돌아간다")
	void memberDeletesWaitingInquiry() {
		String image = upload(MEMBER_A);
		long id = createInquiry(MEMBER_A, "지울 문의", List.of(image));
		assertThat(imageStatus(image)).isEqualTo("USED");

		assertThat(asMember(MEMBER_A, mvc.delete().uri(INQUIRIES + "/" + id))).hasStatus(204);

		assertThat(asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/me"))).bodyJson()
			.extractingPath("$.content.length()").isEqualTo(0);
		assertThat(asAdmin(mvc.get().uri(ADMIN_INQUIRIES).param("status", "ALL"))).bodyJson()
			.extractingPath("$.content.length()").isEqualTo(0);
		assertThat(jdbcTemplate.queryForObject("select count(*) from tb_inquiry_image", Integer.class)).isZero();
		assertThat(imageStatus(image)).isEqualTo("TEMP");
	}

	@Test
	@DisplayName("US3-AC6 답변 완료 문의는 지울 수 없고(B001) 상세의 지우기 가능이 거짓이다")
	void answeredInquiryCannotBeDeleted() {
		long id = createInquiry(MEMBER_A, "답변한 문의", List.of());
		asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>답</p>\"}"));

		assertThat(asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/" + id))).bodyJson()
			.extractingPath("$.deletable").isEqualTo(false);
		assertThat(asMember(MEMBER_A, mvc.delete().uri(INQUIRIES + "/" + id))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US3-AC7 다른 회원의 문의는 없는 문의(R001)로 보이고 지울 수도 없다")
	void otherMembersInquiryIsHidden() {
		long id = createInquiry(MEMBER_A, "A의 문의", List.of());

		assertThat(asMember(MEMBER_B, mvc.get().uri(INQUIRIES + "/" + id))).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(asMember(MEMBER_B, mvc.delete().uri(INQUIRIES + "/" + id))).hasStatus(404);
		assertThat(asMember(MEMBER_B, mvc.get().uri(INQUIRIES + "/me"))).bodyJson()
			.extractingPath("$.content.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US3-AC8 관리자 계정은 문의를 쓰거나 내 문의를 볼 수 없다(A005)")
	void managerAccountCannotUseInquiries() {
		assertThat(asAdmin(post(INQUIRIES, inquiry("ACCOUNT", "관리자 문의", "내용", List.of())))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asAdmin(mvc.get().uri(INQUIRIES + "/me"))).hasStatus(403);
	}

	@Test
	@DisplayName("FR-007 사용 안 함 분류나 올리지 않은 이미지 주소로는 문의를 쓸 수 없다(V001)")
	void inquiryValidation() {
		jdbcTemplate
			.update("update tb_code set enabled = false where group_code = 'SUPPORT_CATEGORY' and code = 'ETC'");

		assertThat(asMember(MEMBER_A, post(INQUIRIES, inquiry("ETC", "기타", "내용", List.of())))).hasStatus(400)
			.bodyJson().extractingPath("$.fieldErrors[0].field").isEqualTo("categoryCode");
		assertThat(asMember(MEMBER_A, post(INQUIRIES,
			inquiry("ACCOUNT", "가짜 이미지", "내용", List.of("/api/v1/rich-text/images/not-uploaded")))))
			.hasStatus(400).bodyJson().extractingPath("$.fieldErrors[0].field").isEqualTo("imageUrls");
	}

	// ---- US4 관리자 답변 ----

	@Test
	@DisplayName("US4-AC1 문의 관리는 기본으로 답변 대기 문의만 분류·제목·작성자·쓴 시각과 함께 보인다")
	void adminListDefaultsToWaiting() {
		createInquiry(MEMBER_A, "대기 하나", List.of());
		createInquiry(MEMBER_B, "대기 둘", List.of());
		long answered = createInquiry(MEMBER_A, "답변한 것", List.of());
		asAdmin(put(ADMIN_INQUIRIES + "/" + answered + "/answer", "{\"content\":\"<p>답</p>\"}"));

		MvcTestResult list = asAdmin(mvc.get().uri(ADMIN_INQUIRIES));

		assertThat(list).hasStatus(200);
		assertThat(list).bodyJson().extractingPath("$.content[*].title").asArray()
			.containsExactly("대기 둘", "대기 하나");
		assertThat(list).bodyJson().extractingPath("$.content[0].author.loginId").isEqualTo("member-b");
		assertThat(list).bodyJson().extractingPath("$.content[0].categoryName").isEqualTo("계정");
		assertThat(list).bodyJson().extractingPath("$.content[0].createdAt").isNotNull();
	}

	@Test
	@DisplayName("US4-AC3 답변을 저장하면 답변 완료가 되고 답변 시각이 남으며 회원 쪽에도 보인다")
	void answerCompletesInquiry() {
		long id = createInquiry(MEMBER_A, "답변할 문의", List.of());

		assertThat(asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>답변</p>\"}")))
			.hasStatus(204);

		MvcTestResult detail = asAdmin(mvc.get().uri(ADMIN_INQUIRIES + "/" + id));
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo("ANSWERED");
		assertThat(detail).bodyJson().extractingPath("$.answeredAt").isNotNull();
		assertThat(detail).bodyJson().extractingPath("$.answeredByName").isEqualTo("admin");
		assertThat(asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/me"))).bodyJson()
			.extractingPath("$.content[0].status").isEqualTo("ANSWERED");
	}

	@Test
	@DisplayName("US4-AC4 답변하면 문의를 쓴 회원에게 문의 제목 없는 개인 알림이 가고, 연결은 그 문의 상세다")
	void answerNotifiesMember() {
		long id = createInquiry(MEMBER_A, "비밀스러운 제목", List.of());

		asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>답</p>\"}"));

		MvcTestResult notifications = asMember(MEMBER_A, mvc.get().uri("/api/v1/notifications"));
		assertThat(notifications).bodyJson().extractingPath("$.content.length()").isEqualTo(1);
		assertThat(notifications).bodyJson().extractingPath("$.content[0].type").isEqualTo("INQUIRY_ANSWERED");
		assertThat(notifications).bodyJson().extractingPath("$.content[0].title").isEqualTo("1:1 문의에 답변이 등록되었습니다");
		assertThat(notifications).bodyJson().extractingPath("$.content[0].link").isEqualTo("/support/inquiries/" + id);
		assertThat(asMember(MEMBER_B, mvc.get().uri("/api/v1/notifications"))).bodyJson()
			.extractingPath("$.content.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US4-AC5 답변을 고치면 회원에게 고친 답변이 보이고 알림은 다시 가지 않는다")
	void modifyingAnswerDoesNotNotifyAgain() {
		long id = createInquiry(MEMBER_A, "고칠 문의", List.of());
		asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>처음 답</p>\"}"));
		String firstAnsweredAt = JsonPath.read(body(asAdmin(mvc.get().uri(ADMIN_INQUIRIES + "/" + id))),
			"$.answeredAt");

		assertThat(asAdmin(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>고친 답</p>\"}")))
			.hasStatus(204);

		MvcTestResult detail = asMember(MEMBER_A, mvc.get().uri(INQUIRIES + "/" + id));
		assertThat(detail).bodyJson().extractingPath("$.answer").asString().contains("고친 답");
		assertThat(detail).bodyJson().extractingPath("$.answeredAt").isEqualTo(firstAnsweredAt);
		assertThat(asMember(MEMBER_A, mvc.get().uri("/api/v1/notifications"))).bodyJson()
			.extractingPath("$.content.length()").isEqualTo(1);
	}

	@Test
	@DisplayName("US4-AC6 문의 관리 권한이 없는 운영자는 문의 목록을 열거나 답변할 수 없다(A005)")
	void operatorWithoutInquiryPermissionIsForbidden() {
		long id = createInquiry(MEMBER_A, "문의", List.of());
		String faqOnly = operator(31L, "faq-op", "FAQ_MANAGE");

		assertThat(mvc.get().uri(ADMIN_INQUIRIES).header(HttpHeaders.AUTHORIZATION, faqOnly).exchange())
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(put(ADMIN_INQUIRIES + "/" + id + "/answer", "{\"content\":\"<p>답</p>\"}")
			.header(HttpHeaders.AUTHORIZATION, faqOnly).exchange()).hasStatus(403);
	}

	@Test
	@DisplayName("US4-AC7 분류, 기간, 검색어(제목·작성자)로 거른다")
	void adminFilters() {
		createInquiry(MEMBER_A, "로그인 문제", List.of());
		asMember(MEMBER_B, post(INQUIRIES, inquiry("ERROR", "화면이 깨져요", "내용", List.of())));
		String today = LocalDate.now().toString();

		assertThat(adminTitles("categoryCode", "ERROR")).containsExactly("화면이 깨져요");
		assertThat(adminTitles("keyword", "로그인")).containsExactly("로그인 문제");
		assertThat(adminTitles("keyword", "member-b")).containsExactly("화면이 깨져요");
		assertThat(titles(asAdmin(mvc.get().uri(ADMIN_INQUIRIES).param("from", today).param("to", today))))
			.containsExactlyInAnyOrder("로그인 문제", "화면이 깨져요");
		assertThat(
			titles(asAdmin(mvc.get().uri(ADMIN_INQUIRIES).param("from", "2000-01-01").param("to", "2000-01-02"))))
			.isEmpty();
	}

	// ---- 도우미 ----

	private long createFaq(String categoryCode, String question, int sortOrder, boolean enabled) {
		return id(asAdmin(post(ADMIN_FAQS, faq(categoryCode, question, "<p>답</p>", sortOrder, enabled))));
	}

	private long createInquiry(long memberId, String title, List<String> imageUrls) {
		return id(asMember(memberId, post(INQUIRIES, inquiry("ACCOUNT", title, "내용", imageUrls))));
	}

	private static String faq(String categoryCode, String question, String answer, int sortOrder, boolean enabled) {
		return "{\"categoryCode\":\"" + categoryCode + "\",\"question\":\"" + question + "\",\"answer\":\""
			+ answer.replace("\"", "\\\"") + "\",\"sortOrder\":" + sortOrder + ",\"enabled\":" + enabled + "}";
	}

	private static String inquiry(String categoryCode, String title, String content, List<String> imageUrls) {
		String images = String.join(",", imageUrls.stream().map(url -> "\"" + url + "\"").toList());
		return "{\"categoryCode\":\"" + categoryCode + "\",\"title\":\"" + title + "\",\"content\":\""
			+ content.replace("\n", "\\n") + "\",\"imageUrls\":[" + images + "]}";
	}

	/** rich-text 이미지 올리기로 회원이 올리고 받은 주소 */
	private String upload(long memberId) {
		MvcTestResult result = mvc.post().uri(IMAGES).multipart()
			.file(new MockMultipartFile("file", "shot.png", "application/octet-stream", PNG))
			.header(HttpHeaders.AUTHORIZATION, member(memberId))
			.exchange();
		assertThat(result).hasStatus(201);
		return JsonPath.read(body(result), "$.url");
	}

	private String imageStatus(String url) {
		return jdbcTemplate.queryForObject("SELECT status FROM tb_rich_text_image WHERE public_id = ?", String.class,
			url.substring(url.lastIndexOf('/') + 1));
	}

	private String member(long memberId) {
		return bearer(memberId, Role.USER.name());
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

	private MvcTestResult asMember(long memberId, MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, member(memberId)).exchange();
	}

	private List<String> questions(MvcTestResult result) {
		return JsonPath.read(body(result), "$[*].question");
	}

	private List<String> adminQuestions() {
		return questions(asAdmin(mvc.get().uri(ADMIN_FAQS)));
	}

	private List<String> titles(MvcTestResult result) {
		return JsonPath.read(body(result), "$.content[*].title");
	}

	private List<String> adminTitles(String param, String value) {
		return titles(asAdmin(mvc.get().uri(ADMIN_INQUIRIES).param("status", "ALL").param(param, value)));
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
