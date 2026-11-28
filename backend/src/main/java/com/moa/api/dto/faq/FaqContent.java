package com.moa.api.dto.faq;

/**
 * FAQ를 만들거나 고칠 때 넣는 칸 (specs/017-customer-center FR-004). 답은 걸러진 HTML이다.
 */
public record FaqContent(String categoryCode, String question, String answer, int sortOrder, boolean enabled) {
}
