package com.moa.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;

import com.moa.api.dto.MemberPolicy;
import com.moa.api.entity.Member;
import com.moa.api.entity.MemberStatus;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.SignUpReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.SecureTokens;

/**
 * 가입 단계와 가입 완료가 함께 쓰는 규칙 (회원 _policy.md 가입 나이, 1인 1계정, 휴대폰 번호 중복 안내).
 */
final class SignUpRules {

	/** 나이는 서비스 지역(한국) 날짜로 센다 */
	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
	private static final int MASK_VISIBLE = 2;
	private static final Map<MemberStatus, ErrorCode> PHONE_OWNER_ERRORS = Map.of(
		MemberStatus.ACTIVE, ErrorCode.PHONE_ALREADY_REGISTERED,
		MemberStatus.SUSPENDED, ErrorCode.PHONE_OWNER_SUSPENDED,
		MemberStatus.WITHDRAWN, ErrorCode.PHONE_OWNER_WITHDRAWN,
		MemberStatus.PURGED, ErrorCode.PHONE_REJOIN_RESTRICTED);

	private SignUpRules() {
	}

	/**
	 * 유효한 가입 진행을 읽는다.
	 *
	 * @throws ServiceException 토큰이 없거나, 가입 진행이 없거나 만료됐으면 A006
	 */
	static SignUp validSignUp(SignUpReader signUpReader, String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new ServiceException(ErrorCode.SIGN_UP_REQUIRED);
		}
		return signUpReader.findByTokenHash(SecureTokens.sha256(rawToken))
			.filter(signUp -> !signUp.isExpired(LocalDateTime.now()))
			.orElseThrow(() -> new ServiceException(ErrorCode.SIGN_UP_REQUIRED));
	}

	/**
	 * 동시 요청으로 유니크 제약에 걸리면 앞서 검사한 업무 오류와 같은 코드로 바꾼다.
	 *
	 * @return 제약 이름으로 정한 오류. 모르는 제약이면 S001
	 */
	static ServiceException uniqueViolation(DataIntegrityViolationException ex) {
		String message = String.valueOf(ex.getMostSpecificCause().getMessage());
		ErrorCode code = ErrorCode.INTERNAL_ERROR;
		if (message.contains("uk_members_nickname")) {
			code = ErrorCode.NICKNAME_TAKEN;
		} else if (message.contains("uk_members_email")) {
			code = ErrorCode.EMAIL_ALREADY_REGISTERED;
		} else if (message.contains("uk_members_phone_hmac")) {
			code = ErrorCode.PHONE_ALREADY_REGISTERED;
		} else if (message.contains("uk_member_social_accounts")) {
			code = ErrorCode.PROVIDER_EMAIL_CONFLICT;
		}
		return new ServiceException(code);
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * @throws ServiceException 만 14세 미만이면 B010
	 */
	static void checkAge(LocalDate birthDate) {
		int age = Period.between(birthDate, LocalDate.now(SERVICE_ZONE)).getYears();
		if (age < MemberPolicy.MIN_SIGN_UP_AGE) {
			throw new ServiceException(ErrorCode.UNDER_SIGN_UP_AGE);
		}
	}

	/**
	 * 휴대폰 번호가 다른 계정과 겹치면 그 계정 상태에 맞는 오류로 막는다.
	 *
	 * @throws ServiceException ACTIVE B011, SUSPENDED B012, WITHDRAWN B013, PURGED(재가입 제한) B014
	 */
	static void checkPhoneOwner(Optional<Member> owner) {
		owner.ifPresent(member -> {
			throw new ServiceException(PHONE_OWNER_ERRORS.get(member.getStatus()));
		});
	}

	/** 가린 이메일을 보여 주는 상태. 탈퇴 유예·파기 계정은 이메일을 보이지 않는다 */
	static boolean showsMaskedEmail(MemberStatus status) {
		return Set.of(MemberStatus.ACTIVE, MemberStatus.SUSPENDED).contains(status);
	}

	/** 예: abcdef@naver.com → ab***@naver.com */
	static String maskEmail(String email) {
		int at = email.indexOf('@');
		if (at < 0) {
			return "***";
		}
		String local = email.substring(0, at);
		int visibleLength = local.length() > MASK_VISIBLE ? MASK_VISIBLE : Math.min(1, local.length());
		String visible = local.substring(0, visibleLength);
		return visible + "***" + email.substring(at);
	}

	/** 로그인 수단 이름 (용어집 LoginMethod) */
	static String loginMethod(OAuthProvider provider) {
		return provider == null ? "PASSWORD" : provider.name();
	}

	static AuthPrincipal principalOf(Member member) {
		return new AuthPrincipal(member.getId(), Set.of(member.getRole().name()));
	}

	static Instant toInstant(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant();
	}
}
