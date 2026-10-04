package com.moa.api.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.entity.Member;
import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.MemberReader;
import com.moa.api.repository.reader.MemberSocialAccountReader;
import com.moa.api.repository.reader.SignUpReader;
import com.moa.api.repository.writer.MemberSocialAccountWriter;
import com.moa.api.repository.writer.MemberWriter;
import com.moa.api.repository.writer.SignUpWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.logging.AuthAuditLogger;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;

/**
 * 회원 만들기 (가입 완료).
 */
@Service
@RequiredArgsConstructor
public class MemberService {

	private final SignUpReader signUpReader;
	private final SignUpWriter signUpWriter;
	private final MemberReader memberReader;
	private final MemberWriter memberWriter;
	private final MemberSocialAccountReader memberSocialAccountReader;
	private final MemberSocialAccountWriter memberSocialAccountWriter;
	private final AuthTokenService authTokenService;
	private final AuthAuditLogger auditLogger;

	/**
	 * 가입 진행을 마치고 ACTIVE 회원을 만든 뒤 로그인 세션을 발급한다. 가입 진행은 지운다.
	 * 필수 동의는 요청 검증(V001)이 먼저 막는다.
	 *
	 * @throws ServiceException A006, B015 인증 미완료, B010, B011~B014, B002 이메일 이미 가입(이메일 가입),
	 *     B003 이메일 이미 가입·제공자 계정 이미 연결(구글·카카오 가입), R003 닉네임 중복. 동시 요청으로 유니크 제약에 걸려도 같은 코드로 답한다
	 */
	@Transactional
	public IssuedTokens createMember(String signUpToken, String nickname) {
		SignUp signUp = SignUpRules.validSignUp(signUpReader, signUpToken);
		if (signUp.getEmail() == null || !signUp.isPhoneVerified() || signUp.getBirthDate() == null) {
			throw new ServiceException(ErrorCode.SIGN_UP_INCOMPLETE);
		}
		SignUpRules.checkAge(signUp.getBirthDate());
		SignUpRules.checkPhoneOwner(memberReader.findByPhoneHmac(signUp.getPhoneHmac()));
		if (memberReader.findByEmail(signUp.getEmail()).isPresent()) {
			throw new ServiceException(signUp.getProvider() == null
				? ErrorCode.EMAIL_ALREADY_REGISTERED
				: ErrorCode.PROVIDER_EMAIL_CONFLICT);
		}
		if (memberReader.existsByNickname(nickname)) {
			throw new ServiceException(ErrorCode.NICKNAME_TAKEN);
		}
		if (signUp.getProvider() != null
			&& memberSocialAccountReader.find(signUp.getProvider(), signUp.getProviderUserId()).isPresent()) {
			throw new ServiceException(ErrorCode.PROVIDER_EMAIL_CONFLICT);
		}

		Member member;
		try {
			member = memberWriter.create(Member.createActive(signUp.getEmail(), signUp.getPasswordHash(),
				nickname, signUp.getBirthDate(), signUp.getPhoneEncrypted(), signUp.getPhoneHmac(),
				LocalDateTime.now()));
			if (signUp.getProvider() != null) {
				memberSocialAccountWriter.create(
					new MemberSocialAccount(member.getId(), signUp.getProvider(), signUp.getProviderUserId()));
			}
		} catch (DataIntegrityViolationException ex) {
			throw SignUpRules.uniqueViolation(ex, signUp.getProvider() == null
				? ErrorCode.EMAIL_ALREADY_REGISTERED
				: ErrorCode.PROVIDER_EMAIL_CONFLICT);
		}
		signUpWriter.delete(signUp);
		IssuedTokens tokens = authTokenService.issueWithNewSession(SignUpRules.principalOf(member));
		auditLogger.signInSucceeded(member.getId(), SignUpRules.loginMethod(signUp.getProvider()), "SIGN_UP");
		return tokens;
	}
}
