package com.moa.api.controller.v1.notice.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "새 공지 여부")
public record NoticeNewResponse(
	@Schema(description = "공지 목록을 마지막으로 연 뒤 게시된 공지가 있다") boolean hasNew) {
}
