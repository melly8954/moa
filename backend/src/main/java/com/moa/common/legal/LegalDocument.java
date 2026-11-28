package com.moa.common.legal;

import java.util.Arrays;
import java.util.Optional;

/**
 * 누구나 여는 공개 문서 (specs/012-terms-privacy data-model). 키는 주소와 문안 폴더 이름이다.
 */
public enum LegalDocument {

	/** 이용약관. 010 가입 화면의 SERVICE와 같은 원문 (research R2) */
	TERMS("terms"),
	/** 개인정보처리방침. 010의 PRIVACY(개인정보 수집·이용 동의)와 다른 문서다 */
	PRIVACY("privacy");

	private final String key;

	LegalDocument(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public static Optional<LegalDocument> fromKey(String key) {
		return Arrays.stream(values()).filter(document -> document.key.equals(key)).findFirst();
	}
}
