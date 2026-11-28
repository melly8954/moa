package com.moa.common.legal;

/**
 * 문서의 장 하나.
 *
 * @param id 장 바로 가기 주소(#id)
 * @param heading 장 제목 (h2 글자). 목차에 쓴다
 * @param html h2를 뺀 장 안의 HTML
 */
public record LegalSection(String id, String heading, String html) {
}
