package com.moa.common.helper.richtext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * 받는 이미지 형식 (선택 헬퍼 rich-text). 이름·Content-Type이 아니라 파일 앞부분(매직 넘버)으로 판별한다.
 * SVG는 안에 스크립트를 넣을 수 있어 받지 않는다.
 */
public enum ImageType {

	PNG("image/png", "png"),
	JPEG("image/jpeg", "jpg"),
	GIF("image/gif", "gif"),
	WEBP("image/webp", "webp");

	/** 판별에 필요한 앞부분 길이 */
	static final int HEADER_LENGTH = 12;

	private final String contentType;
	private final String extension;

	ImageType(String contentType, String extension) {
		this.contentType = contentType;
		this.extension = extension;
	}

	public String contentType() {
		return contentType;
	}

	public String extension() {
		return extension;
	}

	static Optional<ImageType> detect(byte[] header) {
		if (startsWith(header, new byte[] {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
			return Optional.of(PNG);
		}
		if (startsWith(header, new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF})) {
			return Optional.of(JPEG);
		}
		if (startsWith(header, ascii("GIF87a")) || startsWith(header, ascii("GIF89a"))) {
			return Optional.of(GIF);
		}
		if (startsWith(header, ascii("RIFF")) && header.length >= HEADER_LENGTH
			&& Arrays.equals(Arrays.copyOfRange(header, 8, 12), ascii("WEBP"))) {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
	}

	private static byte[] ascii(String value) {
		return value.getBytes(StandardCharsets.US_ASCII);
	}
}
