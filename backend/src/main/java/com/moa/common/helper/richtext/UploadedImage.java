package com.moa.common.helper.richtext;

/**
 * 올린 본문 이미지 (선택 헬퍼 rich-text).
 *
 * @param id 주소에 쓰는 ID
 * @param url 본문 img src에 그대로 넣을 주소 (HtmlSanitizer.IMAGE_PATH_PREFIX + id)
 */
public record UploadedImage(String id, String url) {
}
