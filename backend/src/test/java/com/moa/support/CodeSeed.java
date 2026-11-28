package com.moa.support;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 공통코드를 처음 배포할 때의 구성으로 되돌린다. DatabaseCleaner가 시드를 지우므로 되돌린다 (specs/004-admin-codes research R3).
 * 시드 값을 여기에 다시 적지 않는다: 마이그레이션이 끝난 직후의 tb_code_group·tb_code 표를 한 번 찍어 두고 그대로 되돌린다
 * (MenuSeed와 같은 방식). 공통코드를 넣는 기능이 늘어도 이 클래스는 고치지 않는다.
 * 생성 열(active_key)은 값을 넣을 수 없어 뺀다. FlywayMigrationInitializer를 받아 마이그레이션이 끝난 뒤에 찍는다.
 */
@Component
public class CodeSeed {

	private static final List<String> TABLES = List.of("tb_code_group", "tb_code");

	private final JdbcTemplate jdbcTemplate;
	private final Map<String, List<Map<String, Object>>> initialRows;

	public CodeSeed(JdbcTemplate jdbcTemplate, FlywayMigrationInitializer migrated) {
		this.jdbcTemplate = jdbcTemplate;
		this.initialRows = TABLES.stream().collect(Collectors.toMap(table -> table, this::snapshot));
	}

	/** 두 표를 비우고 마이그레이션 직후의 행으로 채운다 */
	public void restore() {
		for (String table : TABLES) {
			jdbcTemplate.update("delete from " + table);
			for (Map<String, Object> row : initialRows.get(table)) {
				List<String> columns = List.copyOf(row.keySet());
				String sql = "insert into " + table + " (" + String.join(", ", columns) + ") values ("
					+ columns.stream().map(column -> "?").collect(Collectors.joining(", ")) + ")";
				jdbcTemplate.update(sql, columns.stream().map(row::get).toArray());
			}
		}
	}

	private List<Map<String, Object>> snapshot(String table) {
		List<String> columns = jdbcTemplate.queryForList(
			"select column_name from information_schema.columns where table_schema = database() and table_name = ? "
				+ "and is_generated = 'NEVER' order by ordinal_position",
			String.class, table);
		return jdbcTemplate.queryForList("select " + String.join(", ", columns) + " from " + table + " order by id");
	}
}
