package com.moa.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.audit.SystemActor;
import com.moa.support.AcceptanceTest;
import com.moa.support.sample.SampleItem;
import com.moa.support.sample.SampleItemRepository;

/**
 * 감사 행위자와 소프트 삭제 (harness-psw 4.7 공통 엔터티)
 */
class AuditAcceptanceTest extends AcceptanceTest {

	private static final long SYSTEM_ACTOR_ID = 1L;

	@Autowired
	private SampleItemRepository sampleItemRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("로그인 사용자가 저장하면 행위자는 그 사용자다")
	void authenticatedActor() {
		assertThat(create("/test/items", bearer(7L)))
			.hasStatus(200)
			.bodyJson().extractingPath("$.createdBy").isEqualTo(7);
	}

	@Test
	@DisplayName("@PermitAll API에서 로그인 없이 저장하면 행위자는 시스템 계정이다")
	void permitAllActor() {
		assertThat(create("/test/items/public", null))
			.hasStatus(200)
			.bodyJson().extractingPath("$.createdBy").isEqualTo((int)SYSTEM_ACTOR_ID);
	}

	@Test
	@DisplayName("SystemActor 안에서 저장하면 행위자는 시스템 계정이다")
	void systemActor() {
		SampleItem saved = SystemActor.call(() -> sampleItemRepository.save(new SampleItem("batch")));

		assertThat(saved.getCreatedBy()).isEqualTo(SYSTEM_ACTOR_ID);
	}

	@Test
	@DisplayName("행위자 없이 저장하면 실패한다")
	void noActor() {
		assertThatThrownBy(() -> sampleItemRepository.save(new SampleItem("orphan")))
			.satisfies(ex -> assertThat(NestedExceptionUtils.getMostSpecificCause(ex))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("감사 행위자"));
	}

	@Test
	@DisplayName("삭제하면 조회에서 빠지고 행은 삭제자와 함께 남는다")
	void softDelete() throws Exception {
		MvcTestResult created = create("/test/items", bearer(7L));
		Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		assertThat(mvc.delete().uri("/test/items/" + id).header(HttpHeaders.AUTHORIZATION, bearer(8L)))
			.hasStatus(204);
		assertThat(mvc.get().uri("/test/items/" + id).header(HttpHeaders.AUTHORIZATION, bearer(7L)))
			.hasStatus(404);
		Long deletedBy = jdbcTemplate.queryForObject("select deleted_by from sample_items where id = ?", Long.class,
			id.longValue());
		assertThat(deletedBy).isEqualTo(8L);
	}

	private MvcTestResult create(String uri, String authorization) {
		var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"item\"}");
		if (authorization != null) {
			request.header(HttpHeaders.AUTHORIZATION, authorization);
		}
		return request.exchange();
	}
}
