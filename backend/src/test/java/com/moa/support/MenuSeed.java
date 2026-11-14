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
 * 마이그레이션 파일의 INSERT·UPDATE 문을 순서대로 다시 실행한다
 * (moa V2가 키트 시드의 메뉴를 최상위로 옮기고, V2_2가 공지 관리를 더한다).
 * 시드 값을 여기에 다시 적지 않아 시드가 바뀌면 테스트가 따라간다.
 */
@Component
@RequiredArgsConstructor
public class MenuSeed {

	private static final List<String> SCRIPTS = List.of(
		"db/migration/V1_7__menus.sql",
		"db/migration/V1_7_1__admin_menus.sql",
		"db/migration/V2__flat_admin_menus.sql",
		"db/migration/V2_2__notice_admin_menu.sql");

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
				.filter(statement -> statement.startsWith("INSERT") || statement.startsWith("UPDATE"))
				.toList();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
