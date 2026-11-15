package com.moa.api.program;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.common.security.permission.PermissionRegistry;

/**
 * 등록된 프로그램 전부 (moa, specs/001-admin-menus FR-008). 시작할 때 검사하고 틀리면 시작이 실패한다:
 * 키 형식, 키·경로 겹침, 관리 프로그램은 등록된 권한 하나, 사용자 프로그램은 권한 없음.
 */
@Component
public class ProgramRegistry {

	/** 경로가 맞는 프로그램이 없던 메뉴에 마이그레이션이 넣는 키. 등록할 수 없다 */
	public static final String MISSING = "MISSING";

	private static final Pattern KEY = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");

	private final Map<String, ProgramDefinition> definitions = new LinkedHashMap<>();

	public ProgramRegistry(ObjectProvider<ProgramSource> sources, PermissionRegistry permissionRegistry) {
		sources.orderedStream()
			.flatMap(source -> source.programs().stream())
			.forEach(definition -> register(definition, permissionRegistry));
	}

	/** 관리 프로그램 먼저, 그 안에서는 등록 순서 */
	public List<ProgramDefinition> all() {
		return definitions.values().stream()
			.sorted(Comparator.comparing(ProgramDefinition::audience))
			.toList();
	}

	public Optional<ProgramDefinition> find(String key) {
		return Optional.ofNullable(key == null ? null : definitions.get(key));
	}

	public List<ProgramDefinition> of(MenuAudience audience) {
		return all().stream().filter(program -> program.audience() == audience).toList();
	}

	private void register(ProgramDefinition program, PermissionRegistry permissionRegistry) {
		String key = program.key();
		if (key == null || !KEY.matcher(key).matches() || MISSING.equals(key)) {
			throw new IllegalStateException("프로그램 키는 대문자·숫자·밑줄이어야 합니다: " + key);
		}
		if (program.name() == null || program.name().isBlank() || program.audience() == null) {
			throw new IllegalStateException("프로그램 이름과 대상이 필요합니다: " + key);
		}
		if (program.path() == null || !program.path().startsWith("/") || program.path().startsWith("//")) {
			throw new IllegalStateException("프로그램 경로는 /로 시작하는 내부 경로여야 합니다: " + key);
		}
		if (program.audience() == MenuAudience.ADMIN) {
			if (program.permission() == null || !permissionRegistry.contains(program.permission())) {
				throw new IllegalStateException("관리 프로그램은 등록된 권한이 하나 있어야 합니다: " + key);
			}
		} else if (program.permission() != null) {
			throw new IllegalStateException("사용자 프로그램은 권한을 갖지 않습니다: " + key);
		}
		if (definitions.values().stream().map(ProgramDefinition::path).anyMatch(program.path()::equals)) {
			throw new IllegalStateException("프로그램 경로가 겹칩니다: " + program.path());
		}
		if (definitions.putIfAbsent(key, program) != null) {
			throw new IllegalStateException("프로그램 키가 겹칩니다: " + key);
		}
	}
}
