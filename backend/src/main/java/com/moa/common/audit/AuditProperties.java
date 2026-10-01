package com.moa.common.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * @param systemActorId 로그인 전 요청과 시스템 작업의 감사 행위자. 사용자 도메인이 만든 시스템 계정의 ID
 */
@Validated
@ConfigurationProperties("psw.audit")
public record AuditProperties(@NotNull Long systemActorId) {
}
