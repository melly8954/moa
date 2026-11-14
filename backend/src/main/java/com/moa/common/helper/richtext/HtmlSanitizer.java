package com.moa.common.helper.richtext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * 서식 있는 글(HTML) 거르기 (선택 헬퍼 rich-text). 허용 목록 밖의 태그·속성은 지운다.
 * 편집기를 거치지 않고 API로 바로 보낼 수 있으므로 저장할 때 서버에서 거른다 (XSS 방어선).
 *
 * <ul>
 *   <li>태그: p, h2, h3, strong, em, s, ul, ol, li, blockquote, a, img, br, code, pre, hr</li>
 *   <li>a: href(http·https·mailto만). rel="noopener noreferrer"를 붙인다</li>
 *   <li>img: src(우리 파일 주소 {@link #IMAGE_PATH_PREFIX}로 시작하는 것만, 아니면 img를 지운다), alt</li>
 *   <li>style, on* 속성, script, iframe, javascript: 주소 등은 모두 지운다</li>
 * </ul>
 *
 * <pre>{@code
 * String safe = HtmlSanitizer.clean(request.content());   // 또는 요청 DTO 칸에 @SafeHtml
 * }</pre>
 */
public final class HtmlSanitizer {

	/** 본문 이미지로 허용하는 주소의 시작. 이 헬퍼의 본문 이미지 API(ApiRichTextImageController)와 같다 */
	public static final String IMAGE_PATH_PREFIX = "/api/v1/rich-text/images/";

	/** 접두사 뒤는 경로 글자만 (../, //, 쿼리·스킴 없이) */
	private static final Pattern IMAGE_PATH = Pattern
		.compile(Pattern.quote(IMAGE_PATH_PREFIX) + "(?!.*\\.\\.)[A-Za-z0-9._~/-]+");

	private static final Safelist SAFELIST = new Safelist()
		.addTags("p", "h2", "h3", "strong", "em", "s", "ul", "ol", "li", "blockquote", "a", "img", "br", "code",
			"pre", "hr")
		.addAttributes("a", "href")
		.addProtocols("a", "href", "http", "https", "mailto")
		.addEnforcedAttribute("a", "rel", "noopener noreferrer")
		.addAttributes("img", "src", "alt");

	private HtmlSanitizer() {
		// 정적 메서드만 쓴다
	}

	/**
	 * 허용 목록으로 거른 HTML. null이면 null, 거른 뒤 남은 것이 없으면 빈 문자열.
	 */
	public static String clean(String html) {
		if (html == null) {
			return null;
		}
		Document dirty = Jsoup.parseBodyFragment(html);
		Document cleaned = new Cleaner(SAFELIST).clean(dirty);
		for (Element image : cleaned.select("img")) {
			if (!IMAGE_PATH.matcher(image.attr("src")).matches()) {
				image.remove();
			}
		}
		cleaned.outputSettings().prettyPrint(false);
		return cleaned.body().html();
	}

	/** 본문에 쓰인 본문 이미지 ID (IMAGE_PATH_PREFIX 뒤). 글과 이미지를 이을 때 쓴다 (RichTextImageService.attach) */
	public static Set<String> imageIds(String html) {
		Set<String> ids = new LinkedHashSet<>();
		if (html == null) {
			return ids;
		}
		for (Element image : Jsoup.parseBodyFragment(html).select("img[src]")) {
			String src = image.attr("src");
			if (IMAGE_PATH.matcher(src).matches()) {
				ids.add(src.substring(IMAGE_PATH_PREFIX.length()));
			}
		}
		return ids;
	}

	/** 태그를 뺀 글자만 (목록 미리보기, 검색용). 공백은 하나로 줄인다 */
	public static String plainText(String html) {
		return html == null ? null : Jsoup.parseBodyFragment(html).text();
	}
}
