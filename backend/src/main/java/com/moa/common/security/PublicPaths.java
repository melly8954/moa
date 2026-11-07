package com.moa.common.security;

import java.util.List;

/**
 * 로그인 없이 열어 둘 경로를 더한다. 컨트롤러가 아닌 진입점(WebSocket 핸드셰이크, OAuth 콜백 등)에 쓴다.
 * 빈으로 등록하면 SecurityConfig가 모아 permitAll로 연다. 컨트롤러 API는 @PermitAll을 쓴다.
 */
@FunctionalInterface
public interface PublicPaths {

	List<String> paths();
}
