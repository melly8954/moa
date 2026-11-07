package com.moa.support;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.jwt.JwtTokenProvider;

/**
 * 인수(AC) 테스트 기반. API 수준에서 사용자가 보는 동작을 확인한다.
 *
 * <ul>
 *   <li>DB는 Testcontainers MariaDB. Docker가 실행 중이어야 한다</li>
 *   <li>태그 db다. 기본 test·build에서 빠지고 ./gradlew dbTest와 CI에서 돈다 (로컬 메모리 부담)</li>
 *   <li>요청마다 트랜잭션이 따로 돈다(운영과 같다). 테스트가 끝나면 DatabaseCleaner가 테이블을 비운다</li>
 *   <li>테스트 이름은 사용자가 보는 동작으로 적는다. 예: {@code @DisplayName("주문: 비회원도 주문을 만든다")}</li>
 * </ul>
 */
@Tag("db")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class AcceptanceTest {

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@AfterEach
	void cleanDatabase() {
		databaseCleaner.clean();
	}

	/** Authorization 헤더 값. 로그인 API 없이 인증된 요청을 만든다 */
	protected String bearer(Long userId, String... roles) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthPrincipal(userId, Set.of(roles)));
	}
}
