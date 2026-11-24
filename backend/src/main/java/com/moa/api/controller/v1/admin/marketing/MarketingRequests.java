package com.moa.api.controller.v1.admin.marketing;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.jspecify.annotations.Nullable;

/**
 * 배너·팝업 요청이 같이 쓰는 규칙 (specs/016-banner-popup research R5, R10).
 */
public final class MarketingRequests {

	/**
	 * 연결 주소: 내부 주소(/로 시작하되 //로 시작하지 않음)나 http·https 주소. 빈칸은 받지 않는다(비우려면 보내지 않는다)
	 */
	public static final String LINK_URL_PATTERN = "^(/(?!/)\\S*|https?://\\S+)$";

	public static final String LINK_URL_MESSAGE = "연결 주소는 /로 시작하는 내부 주소나 http·https 주소여야 합니다.";

	private MarketingRequests() {
	}

	/** 요청의 일시(오프셋 포함)를 서버 시간대의 저장 값으로 바꾼다 (응답은 ApiTimes.of로 되돌린다) */
	public static @Nullable LocalDateTime toServerTime(@Nullable OffsetDateTime time) {
		return time == null ? null : time.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
	}
}
