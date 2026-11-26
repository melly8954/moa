package com.moa.common.security.token;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * 파기한 회원의 리프레시 토큰 행을 모두 지운다 (moa 018, specs/018-member-withdrawal research R6).
 * 키트 RefreshTokenRepository·AuthTokenService를 고치지 않으려고 따로 둔다. 부르는 쪽의 트랜잭션 안에서 돈다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenEraser {

	private final EntityManager entityManager;

	/** @return 지운 행 수 */
	public int deleteAllOf(Long userId) {
		return entityManager.createNativeQuery("delete from tb_refresh_token where user_id = :userId")
			.setParameter("userId", userId)
			.executeUpdate();
	}
}
