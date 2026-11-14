package com.moa.common.helper.webpush;

/** 웹 푸시 한 건을 보낸 결과 */
public enum WebPushResult {
	/** 푸시 서버가 받았다 */
	SENT,
	/** 구독이 없어졌다 (404·410). 구독을 지운다 */
	EXPIRED,
	/** 그 밖의 실패. 구독은 두고 다음 알림 때 다시 보낸다 */
	FAILED,
	/** 웹 푸시가 꺼져 있다 (VAPID 키 없음) */
	DISABLED
}
