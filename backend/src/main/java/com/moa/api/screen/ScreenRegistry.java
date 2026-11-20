package com.moa.api.screen;

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
 * 등록된 연결 화면 전부 (moa, specs/001-admin-menus FR-008). 시작할 때 검사하고 틀리면 시작이 실패한다:
 * 키 형식, 키·경로 겹침, 관리 연결 화면은 등록된 권한 하나, 사용자 연결 화면은 권한 없음.
 */
@Component
public class ScreenRegistry {

	/** 경로가 맞는 연결 화면이 없던 메뉴에 마이그레이션이 넣는 키. 등록할 수 없다 */
	public static final String MISSING = "MISSING";

	private static final Pattern KEY = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");

	private final Map<String, ScreenDefinition> definitions = new LinkedHashMap<>();

	public ScreenRegistry(ObjectProvider<ScreenSource> sources, PermissionRegistry permissionRegistry) {
		sources.orderedStream()
			.flatMap(source -> source.screens().stream())
			.forEach(definition -> register(definition, permissionRegistry));
	}

	/** 관리 연결 화면 먼저, 그 안에서는 등록 순서 */
	public List<ScreenDefinition> all() {
		return definitions.values().stream()
			.sorted(Comparator.comparing(ScreenDefinition::audience))
			.toList();
	}

	public Optional<ScreenDefinition> find(String key) {
		return Optional.ofNullable(key == null ? null : definitions.get(key));
	}

	public List<ScreenDefinition> of(MenuAudience audience) {
		return all().stream().filter(screen -> screen.audience() == audience).toList();
	}

	private void register(ScreenDefinition screen, PermissionRegistry permissionRegistry) {
		String key = screen.key();
		if (key == null || !KEY.matcher(key).matches() || MISSING.equals(key)) {
			throw new IllegalStateException("연결 화면 키는 대문자·숫자·밑줄이어야 합니다: " + key);
		}
		if (screen.name() == null || screen.name().isBlank() || screen.audience() == null) {
			throw new IllegalStateException("연결 화면 이름과 대상이 필요합니다: " + key);
		}
		if (screen.path() == null || !screen.path().startsWith("/") || screen.path().startsWith("//")) {
			throw new IllegalStateException("연결 화면 경로는 /로 시작하는 내부 경로여야 합니다: " + key);
		}
		if (screen.audience() == MenuAudience.ADMIN) {
			if (screen.permission() == null || !permissionRegistry.contains(screen.permission())) {
				throw new IllegalStateException("관리 연결 화면은 등록된 권한이 하나 있어야 합니다: " + key);
			}
		} else if (screen.permission() != null) {
			throw new IllegalStateException("사용자 연결 화면은 권한을 갖지 않습니다: " + key);
		}
		if (definitions.values().stream().map(ScreenDefinition::path).anyMatch(screen.path()::equals)) {
			throw new IllegalStateException("연결 화면 경로가 겹칩니다: " + screen.path());
		}
		if (definitions.putIfAbsent(key, screen) != null) {
			throw new IllegalStateException("연결 화면 키가 겹칩니다: " + key);
		}
	}
}
