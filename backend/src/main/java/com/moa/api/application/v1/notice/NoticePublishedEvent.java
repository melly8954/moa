package com.moa.api.application.v1.notice;

/** 공지를 게시했다. 커밋 뒤 전체 알림으로 보낸다 (specs/007-notifications R4, 006 R9) */
public record NoticePublishedEvent(Long id, String title) {
}
