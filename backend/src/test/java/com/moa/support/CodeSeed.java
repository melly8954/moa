package com.moa.support;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 공통코드를 처음 배포할 때의 구성으로 되돌린다 (MenuSeed와 같은 방식). DatabaseCleaner가 마이그레이션 시드까지 지우므로 다시 넣는다.
 * 마이그레이션이 끝난 직후의 tb_code_group·tb_code 표를 한 번 찍어 두고 그대로 되돌린다.
 * 가상 열(active_key)은 넣을 수 없으므로 뺀다.
 */
@Component
public class CodeSeed {

	private static final List<String> TABLES = List.of("tb_code_group", "tb_code");

	private final JdbcTemplate jdbcTemplate;
	private final Map<String, List<Map<String, Object>>> initialRows;

	public CodeSeed(JdbcTemplate jdbcTemplate, FlywayMigrationInitializer migrated) {
		this.jdbcTemplate = jdbcTemplate;
		this.initialRows = TABLES.stream()
			.collect(Collectors.toMap(table -> table,
				table -> jdbcTemplate.queryForList("select * from " + table + " order by id")));
	}

	public void restore() {
		for (String table : TABLES) {
			jdbcTemplate.update("delete from " + table);
			for (Map<String, Object> row : initialRows.get(table)) {
				List<String> columns = row.keySet().stream().filter(column -> !"active_key".equals(column)).toList();
				String sql = "insert into " + table + " (" + String.join(", ", columns) + ") values ("
					+ columns.stream().map(column -> "?").collect(Collectors.joining(", ")) + ")";
				jdbcTemplate.update(sql, columns.stream().map(row::get).toArray());
			}
		}
	}
}
