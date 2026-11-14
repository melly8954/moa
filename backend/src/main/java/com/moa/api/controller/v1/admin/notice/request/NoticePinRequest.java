package com.moa.api.controller.v1.admin.notice.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 고정·풀기")
public record NoticePinRequest(@Schema(description = "상단 고정") boolean pinned) {
}
