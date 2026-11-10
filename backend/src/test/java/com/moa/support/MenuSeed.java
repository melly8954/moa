package com.moa.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 메뉴를 처음 배포할 때의 구성으로 되돌린다. DatabaseCleaner가 마이그레이션 시드까지 지우므로,
 * 마이그레이션 파일의 INSERT 문을 다시 실행한다. 시드 값을 여기에 다시 적지 않아 시드가 바뀌면 테스트가 따라간다.
 */
@Component
@RequiredArgsConstructor
public class MenuSeed {

	private static final List<String> SCRIPTS = List.of(
		"db/migration/V1_7__menus.sql",
		"db/migration/V1_7_1__admin_menus.sql");

	private final JdbcTemplate jdbcTemplate;

	public void restore() {
		jdbcTemplate.update("delete from menus");
		for (String script : SCRIPTS) {
			for (String insert : insertStatementsOf(script)) {
				jdbcTemplate.update(insert);
			}
		}
	}

	private List<String> insertStatementsOf(String script) {
		try {
			String sql = new ClassPathResource(script).getContentAsString(StandardCharsets.UTF_8)
				.replaceAll("(?m)^\\s*--.*$", "");
			return Arrays.stream(sql.split(";"))
				.map(String::strip)
				.filter(statement -> statement.startsWith("INSERT"))
				.toList();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
