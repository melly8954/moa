package com.moa.api.service.user;

import org.springframework.stereotype.Component;

import com.moa.api.dto.user.UserDto;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.token.AuthPrincipalLoader;

import lombok.RequiredArgsConstructor;

/**
 * 토큰 재발급 때 사용자의 현재 역할을 다시 읽는다. 역할이 바뀌었으면 다음 재발급부터 반영된다.
 * 지운(소프트 삭제) 사용자는 읽히지 않아, 잠긴 사용자는 걸러 재발급이 A003으로 막힌다. 비밀번호가 없는 소셜 계정도 재발급할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class UserPrincipalLoader implements AuthPrincipalLoader {

	private final UserReader userReader;

	@Override
	public AuthPrincipal load(Long userId) {
		return userReader.find(userId)
			.filter(user -> !user.isLocked())
			.map(UserDto::from)
			.map(UserDto::toPrincipal)
			.orElseThrow(() -> new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN));
	}
}
