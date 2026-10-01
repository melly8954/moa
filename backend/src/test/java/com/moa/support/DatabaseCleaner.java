package com.moa.support;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 테스트가 끝날 때 모든 테이블을 비운다. Flyway 이력 테이블은 남긴다.
 * 테스트 전체를 트랜잭션으로 감싸 롤백하지 않는 이유: 요청마다 트랜잭션이 따로 도는 운영과 동작이 달라진다.
 */
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

	private final JdbcTemplate jdbcTemplate;

	public void clean() {
		List<String> tables = jdbcTemplate.queryForList(
			"select table_name from information_schema.tables where table_schema = database() "
				+ "and table_type = 'BASE TABLE' and table_name <> 'flyway_schema_history'",
			String.class);
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
		tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`"));
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
	}
}
