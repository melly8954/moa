package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;

/**
 * 공통코드. 스펙 specs/004-admin-codes의 수용 시나리오다
 */
class AdminCodesAcceptanceTest extends AcceptanceTest {

	/**
	 * spec.md FR-001 표의 코드 그룹. 지금은 표가 "(아직 없음)"이라 비어 있다.
	 * 기능이 코드 그룹을 시드로 넣어 FR-001 표에 더하면 여기에도 더하고, 시드 복원 코드를 함께 둔다 (research.md R3)
	 */
	private static final List<String> FR_001_GROUP_CODES = List.of();

	@Test
	@DisplayName("US1-AC1 관리자가 코드 관리를 열면 FR-001 표의 코드 그룹이 보인다 (처음에는 비어 있다)")
	void adminSeesCodeGroupsOfFr001() {
		MvcTestResult result = mvc.get().uri("/api/v1/admin/code-groups")
			.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN"))
			.exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(FR_001_GROUP_CODES.size());
		assertThat(result).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactlyElementsOf(FR_001_GROUP_CODES);
	}
}
