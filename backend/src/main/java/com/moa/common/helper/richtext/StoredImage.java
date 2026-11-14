package com.moa.common.helper.richtext;

import org.springframework.core.io.Resource;

/** 보여 줄 본문 이미지 (선택 헬퍼 rich-text) */
public record StoredImage(Resource resource, String contentType) {
}
