package com.moa.common.helper.richtext;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * {@link SafeHtml} 칸을 읽을 때 HTML을 거른다 (선택 헬퍼 rich-text).
 */
public class SafeHtmlDeserializer extends ValueDeserializer<String> {

	@Override
	public String deserialize(JsonParser parser, DeserializationContext context) {
		return HtmlSanitizer.clean(parser.getValueAsString());
	}
}
