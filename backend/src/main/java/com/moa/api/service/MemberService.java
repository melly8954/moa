package com.moa.api.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.entity.Member;
import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.MemberReader;
import com.moa.api.repository.reader.MemberSocialAccountReader;
import com.moa.api.repository.writer.MemberSocialAccountWriter;
import com.moa.api.repository.writer.MemberWriter;
import com.moa.api.repository.writer.SignUpWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;

/**
 * 회원 만들기 (가입 완료).
 */
@Service
@RequiredArgsConstructor
public class MemberService {

	private final SignUpService signUpService;
	private final SignUpWriter signUpWriter;
	private final MemberReader memberReader;
	private final MemberWriter memberWriter;
	private final MemberSocialAccountReader memberSocialAccountReader;
	private final MemberSocialAccountWriter memberSocialAccountWriter;
	private final AuthTokenService authTokenService;

	/**
	 * 가입 진행을 마치고 ACTIVE 회원을 만든 뒤 로그인 세션을 발급한다. 가입 진행은 지운다.
	 * 필수 동의는 요청 검증(V001)이 먼저 막는다.
	 *
	 * @throws ServiceException A006, B015 인증 미완료, B010, B011~B014, B002·B003 이메일 이미 가입, R003 닉네임 중복
	 */
	@Transactional
	public IssuedTokens createMember(String signUpToken, String nickname) {
		SignUp signUp = signUpService.getValidSignUp(signUpToken);
		if (signUp.getEmail() == null || !signUp.isPhoneVerified() || signUp.getBirthDate() == null) {
			throw new ServiceException(ErrorCode.SIGN_UP_INCOMPLETE);
		}
		SignUpRules.checkAge(signUp.getBirthDate());
		SignUpRules.checkPhoneOwner(memberReader.findByPhoneHmac(signUp.getPhoneHmac()));
		if (memberReader.findByEmail(signUp.getEmail()).isPresent()) {
			throw new ServiceException(signUp.getProvider() == null
				? ErrorCode.EMAIL_ALREADY_REGISTERED
				: ErrorCode.SOCIAL_EMAIL_CONFLICT);
		}
		if (memberReader.existsByNickname(nickname)) {
			throw new ServiceException(ErrorCode.NICKNAME_TAKEN);
		}
		if (signUp.getProvider() != null
			&& memberSocialAccountReader.find(signUp.getProvider(), signUp.getProviderUserId()).isPresent()) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
		}

		Member member = memberWriter.create(Member.createActive(signUp.getEmail(), signUp.getPasswordHash(),
			nickname, signUp.getBirthDate(), signUp.getPhoneEncrypted(), signUp.getPhoneHmac(),
			LocalDateTime.now()));
		if (signUp.getProvider() != null) {
			memberSocialAccountWriter.create(
				new MemberSocialAccount(member.getId(), signUp.getProvider(), signUp.getProviderUserId()));
		}
		signUpWriter.delete(signUp);
		return authTokenService.issueWithNewSession(SignUpRules.principalOf(member));
	}
}
