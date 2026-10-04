package com.moa.api.application.v1;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.common.audit.SystemActor;

import lombok.RequiredArgsConstructor;

/**
 * 끝내지 않은 가입 진행(유효 30분)과 만료된 인증을 정리한다 (conventions.md 하드 삭제 표).
 */
@Component
@RequiredArgsConstructor
public class SignUpCleanupScheduler {

	private final SignUpApplication signUpApplication;

	@Scheduled(cron = "${psw.sign-up.cleanup-cron:0 15 * * * *}")
	public void deleteExpired() {
		SystemActor.run(signUpApplication::deleteExpired);
	}
}
