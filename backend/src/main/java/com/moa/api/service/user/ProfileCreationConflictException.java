package com.moa.api.service.user;

/**
 * 같은 회원의 프로필 행을 다른 요청이 먼저 만들었다 (specs/009-mypage research R14).
 * tb_user_profile PK(user_id) 충돌, 또는 없는 행을 함께 잠근 두 요청의 교착이다. 부르는 쪽(Application)이 새 트랜잭션으로
 * 다시 하면 행이 이미 있으므로 고치기 경로로 간다. 마지막 시도에서는 서비스가 R002로 바꾸므로 이 예외가 나오지 않는다.
 */
public class ProfileCreationConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfileCreationConflictException(Throwable cause) {
		super("프로필 행 동시 생성", cause);
	}
}
