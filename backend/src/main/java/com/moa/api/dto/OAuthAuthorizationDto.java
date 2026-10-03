package com.moa.api.dto;

/**
 * 제공자 동의 화면으로 보낼 정보.
 *
 * @param authorizationUrl 제공자 동의 화면 주소
 * @param state 콜백에서 확인할 CSRF 방지 값. 쿠키로 브라우저에 둔다
 */
public record OAuthAuthorizationDto(String authorizationUrl, String state) {
}
