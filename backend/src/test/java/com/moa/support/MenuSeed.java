package com.moa.support;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 메뉴를 처음 배포할 때의 구성으로 되돌린다. DatabaseCleaner가 마이그레이션 시드까지 지우므로 다시 넣는다.
 * 시드 값을 여기에 다시 적지 않는다: 마이그레이션이 끝난 직후의 menus 표를 한 번 찍어 두고 그대로 되돌린다.
 * 예전 마이그레이션의 INSERT 문을 다시 실행하면, 뒤의 마이그레이션(V2_5)이 지운 열(path, permission) 때문에 실패한다.
 * FlywayMigrationInitializer를 받아 마이그레이션이 끝난 뒤에 찍는다.
 */
@Component
public class MenuSeed {

	private final JdbcTemplate jdbcTemplate;
	private final List<Map<String, Object>> initialRows;

	public MenuSeed(JdbcTemplate jdbcTemplate, FlywayMigrationInitializer migrated) {
		this.jdbcTemplate = jdbcTemplate;
		this.initialRows = jdbcTemplate.queryForList("select * from menus order by id");
	}

	public void restore() {
		jdbcTemplate.update("delete from menus");
		for (Map<String, Object> row : initialRows) {
			List<String> columns = List.copyOf(row.keySet());
			String sql = "insert into menus (" + String.join(", ", columns) + ") values ("
				+ columns.stream().map(column -> "?").collect(Collectors.joining(", ")) + ")";
			jdbcTemplate.update(sql, columns.stream().map(row::get).toArray());
		}
	}
}
