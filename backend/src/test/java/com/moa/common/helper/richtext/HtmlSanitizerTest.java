package com.moa.common.helper.richtext;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * 서식 있는 글 거르기 (선택 헬퍼 rich-text). DB 없이.
 */
class HtmlSanitizerTest {

	@Test
	@DisplayName("rich-text: 허용한 태그는 남기고 script·iframe·style·on* 속성은 지운다")
	void allowList() {
		String html = "<h2>제목</h2><p style=\"color:red\" onclick=\"x()\">본문 <strong>굵게</strong> <em>기울임</em></p>"
			+ "<script>alert(1)</script><iframe src=\"https://evil.example.com\"></iframe>"
			+ "<ul><li>하나</li></ul><blockquote>인용</blockquote><pre><code>code</code></pre><hr>";

		assertThat(HtmlSanitizer.clean(html)).isEqualTo("<h2>제목</h2><p>본문 <strong>굵게</strong> <em>기울임</em></p>"
			+ "<ul><li>하나</li></ul><blockquote>인용</blockquote><pre><code>code</code></pre><hr>");
	}

	@Test
	@DisplayName("rich-text: 링크는 http·https·mailto만 남기고 rel을 붙인다. javascript: 주소는 href를 지운다")
	void links() {
		assertThat(HtmlSanitizer.clean("<a href=\"https://example.com\" target=\"_blank\">링크</a>"))
			.isEqualTo("<a href=\"https://example.com\" rel=\"noopener noreferrer\">링크</a>");
		assertThat(HtmlSanitizer.clean("<a href=\"javascript:alert(1)\">x</a>"))
			.isEqualTo("<a rel=\"noopener noreferrer\">x</a>");
		assertThat(HtmlSanitizer.clean("<a href=\"mailto:a@example.com\">메일</a>"))
			.contains("href=\"mailto:a@example.com\"");
	}

	@Test
	@DisplayName("rich-text: 이미지는 우리 파일 주소만 남긴다. 외부 주소·data:·../ 주소는 img째 지운다")
	void images() {
		String src = "/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c";
		assertThat(HtmlSanitizer.clean("<img src=\"" + src + "\" alt=\"그림\" onerror=\"x()\">"))
			.isEqualTo("<img src=\"" + src + "\" alt=\"그림\">");
		assertThat(HtmlSanitizer.clean("<p>a<img src=\"https://tracker.example.com/p.gif\">b</p>"))
			.isEqualTo("<p>ab</p>");
		assertThat(HtmlSanitizer.clean("<img src=\"data:image/png;base64,AAAA\">")).isEmpty();
		assertThat(HtmlSanitizer.clean("<img src=\"/api/v1/rich-text/images/../secret.png\">")).isEmpty();
	}

	@Test
	@DisplayName("rich-text: @SafeHtml 칸은 JSON을 읽을 때 걸러지고, plainText는 글자만 준다")
	void safeHtmlAndPlainText() {
		Sample sample = JsonMapper.builder().build()
			.readValue("{\"content\":\"<p>안녕<script>x</script></p>\"}", Sample.class);

		assertThat(sample.content()).isEqualTo("<p>안녕</p>");
		assertThat(HtmlSanitizer.plainText("<p>첫 줄</p><p>둘째 <strong>줄</strong></p>")).isEqualTo("첫 줄 둘째 줄");
		assertThat(HtmlSanitizer.clean(null)).isNull();
		assertThat(
			HtmlSanitizer.imageIds("<p><img src=\"/api/v1/rich-text/images/abc-1\"><img src=\"https://x/y.png\"></p>"))
			.containsExactly("abc-1");
	}

	record Sample(@SafeHtml String content) {
	}
}
