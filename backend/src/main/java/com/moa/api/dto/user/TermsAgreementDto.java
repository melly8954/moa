package com.moa.api.dto.user;

/**
 * 가입 때 동의한 약관 하나 (specs/010-social-login FR-013). 종류·버전은 서비스가 현재 약관과 맞춰 본다.
 *
 * @param type SERVICE 또는 PRIVACY
 */
public record TermsAgreementDto(String type, String version) {
}
