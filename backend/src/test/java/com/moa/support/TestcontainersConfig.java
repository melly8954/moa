package com.moa.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 MariaDB. 운영과 같은 DB로 AC 테스트를 돌린다 (H2 등 대체 DB를 쓰지 않는다).
 * Docker가 실행 중이어야 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

	@Bean
	@ServiceConnection
	public MariaDBContainer<?> mariadb() {
		return new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"));
	}
}
