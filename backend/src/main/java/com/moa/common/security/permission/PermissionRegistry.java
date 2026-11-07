package com.moa.common.security.permission;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 등록된 권한 전부. 등록 순서(빈 순서, 그 안의 목록 순서)를 지킨다. 키 형식이 틀리거나 겹치면 시작이 실패한다.
 */
@Component
public class PermissionRegistry {

	private static final Pattern KEY = Pattern.compile("[A-Z][A-Z0-9_]{0,99}");

	private final Map<String, PermissionDefinition> definitions = new LinkedHashMap<>();

	public PermissionRegistry(ObjectProvider<PermissionSource> sources) {
		sources.orderedStream()
			.flatMap(source -> source.permissions().stream())
			.forEach(this::register);
	}

	public List<PermissionDefinition> all() {
		return List.copyOf(definitions.values());
	}

	/** 등록 순서를 지킨다 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(definitions.keySet());
	}

	public boolean contains(String key) {
		return definitions.containsKey(key);
	}

	private void register(PermissionDefinition definition) {
		if (definition.key() == null || !KEY.matcher(definition.key()).matches()) {
			throw new IllegalStateException("권한 키는 대문자·숫자·밑줄이어야 합니다: " + definition.key());
		}
		if (definitions.putIfAbsent(definition.key(), definition) != null) {
			throw new IllegalStateException("권한 키가 겹칩니다: " + definition.key());
		}
	}
}
