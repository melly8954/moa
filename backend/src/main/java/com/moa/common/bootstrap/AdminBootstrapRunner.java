package com.moa.common.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.user.UserApplication;
import com.moa.common.audit.SystemActor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 첫 관리자를 만든다. 운영 DB에는 로컬 계정이 들어가지 않으므로, 배포할 때 BOOTSTRAP_ADMIN_LOGIN_ID·PASSWORD를 주고 한 번 띄운다.
 *
 * <ul>
 *   <li>ADMIN이 하나도 없을 때만 만든다. 이미 있으면 아무것도 하지 않는다 (환경 변수를 남겨 둬도 다시 만들지 않는다)</li>
 *   <li>만든 계정은 비밀번호 변경 요구 상태다. 첫 로그인 뒤 화면이 비밀번호 변경으로 보낸다</li>
 *   <li>같은 아이디의 일반 계정이 있거나 비밀번호가 8자보다 짧으면 시작이 실패한다 (설정 실수)</li>
 *   <li>설정이 없는데 ADMIN도 없으면 경고 로그만 남긴다</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminBootstrapRunner implements ApplicationRunner {

	private final BootstrapAdminProperties properties;
	private final UserApplication userApplication;

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.configured()) {
			if (!userApplication.hasAdmin()) {
				log.warn("ADMIN 계정이 없습니다. BOOTSTRAP_ADMIN_LOGIN_ID·BOOTSTRAP_ADMIN_PASSWORD를 주고 한 번 띄우면 만듭니다.");
			}
			return;
		}
		boolean created = SystemActor.call(() -> userApplication.bootstrapAdmin(properties.loginId(),
			properties.password(), properties.name()));
		if (created) {
			log.info("첫 관리자 {}를 만들었습니다. 첫 로그인 때 비밀번호를 바꾸고, BOOTSTRAP_ADMIN_* 환경 변수는 지웁니다.",
				properties.loginId());
		} else {
			log.info("ADMIN 계정이 이미 있어 첫 관리자를 만들지 않았습니다. BOOTSTRAP_ADMIN_* 환경 변수는 지웁니다.");
		}
	}
}
