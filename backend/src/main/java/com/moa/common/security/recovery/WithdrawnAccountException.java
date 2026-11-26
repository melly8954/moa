package com.moa.common.security.recovery;

import java.time.LocalDateTime;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.Getter;

/**
 * 본인 확인에 성공했지만 탈퇴해 유예 중인 회원 (A401, specs/018-member-withdrawal research R5).
 * {@link RecoveryExceptionHandler}가 복구 쿠키를 심고 같은 오류 응답을 만든다. 비밀번호가 맞은 다음에만 던진다.
 */
@Getter
public class WithdrawnAccountException extends ServiceException {

	private final transient RecoveryTicket ticket;

	public WithdrawnAccountException(Long userId, LocalDateTime withdrawnAt) {
		super(ErrorCode.ACCOUNT_WITHDRAWN);
		this.ticket = new RecoveryTicket(userId, withdrawnAt);
	}
}
