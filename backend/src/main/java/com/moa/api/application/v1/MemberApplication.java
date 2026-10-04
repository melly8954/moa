package com.moa.api.application.v1;

import org.springframework.stereotype.Component;

import com.moa.api.application.v1.command.CreateMemberCommand;
import com.moa.api.service.MemberService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;

/**
 * 회원 API의 진입점.
 */
@Component
@RequiredArgsConstructor
public class MemberApplication {

	private final MemberService memberService;

	/**
	 * 가입 진행을 마치고 ACTIVE 회원을 만든 뒤 로그인시킨다. 가입 진행은 지운다.
	 *
	 * @throws com.moa.common.exception.ServiceException 가입 진행 토큰이 무효면 A006,
	 *     이메일·휴대폰 인증이나 생년월일이 없으면 B015, 만 14세 미만이면 B010, 휴대폰 번호가 겹치면 B011·B012·B013,
	 *     재가입 제한 중이면 B014, 이메일이 이미 가입돼 있으면 B002(이메일 가입)·B003(구글·카카오 가입),
	 *     제공자 계정이 이미 다른 회원에 연결돼 있으면 B003, 닉네임이 쓰이면 R003
	 */
	public IssuedTokens createMember(CreateMemberCommand command) {
		return memberService.createMember(command.signUpToken(), command.nickname());
	}
}
