package com.moa.api.service.user;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 마이페이지 설정 (specs/009-mypage research R5).
 *
 * @param nicknameCooldown 닉네임을 바꾼 뒤 다시 바꿀 수 있을 때까지 (잠정 FR-004, 기본 30일)
 */
@ConfigurationProperties("app.profile")
public record ProfileProperties(@DefaultValue("30d") Duration nicknameCooldown) {
}
