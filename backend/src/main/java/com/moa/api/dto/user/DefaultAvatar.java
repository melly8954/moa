package com.moa.api.dto.user;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * moa가 제공하는 기본 아바타 (specs/009-mypage FR-009, research R10). 이 순서로 고르기 목록에 보인다.
 * 그림은 프론트 public/avatars/&lt;키&gt;.svg다. 회원은 이 목록 밖의 값을 저장할 수 없다.
 */
public enum DefaultAvatar {
	DEFAULT("default"),
	CAT("cat"),
	DOG("dog"),
	FOX("fox"),
	BEAR("bear"),
	RABBIT("rabbit"),
	PENGUIN("penguin"),
	ROBOT("robot");

	private final String key;

	DefaultAvatar(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public static List<String> keys() {
		return Arrays.stream(values()).map(DefaultAvatar::key).toList();
	}

	public static Optional<DefaultAvatar> find(String key) {
		return Arrays.stream(values()).filter(avatar -> avatar.key.equals(key)).findFirst();
	}

	/** 저장된 키를 보일 키로. 고른 적이 없거나 목록에서 빠진 키는 default (Edge Cases) */
	public static String displayKey(String key) {
		return find(key).orElse(DEFAULT).key;
	}
}
