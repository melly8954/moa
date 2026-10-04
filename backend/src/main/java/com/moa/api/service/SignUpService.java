package com.moa.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.EmailConfirmationDto;
import com.moa.api.dto.EmailConfirmationResult;
import com.moa.api.dto.IssuedEmailVerificationDto;
import com.moa.api.dto.IssuedPhoneVerificationDto;
import com.moa.api.dto.MemberPolicy;
import com.moa.api.dto.PhoneVerificationDto;
import com.moa.api.dto.SignUpDto;
import com.moa.api.entity.EmailVerification;
import com.moa.api.entity.Member;
import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.entity.NotificationTargetType;
import com.moa.api.entity.NotificationType;
import com.moa.api.entity.PhoneVerification;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.EmailVerificationReader;
import com.moa.api.repository.reader.MemberReader;
import com.moa.api.repository.reader.MemberSocialAccountReader;
import com.moa.api.repository.reader.PhoneVerificationReader;
import com.moa.api.repository.reader.SignUpReader;
import com.moa.api.repository.writer.EmailVerificationWriter;
import com.moa.api.repository.writer.MemberSocialAccountWriter;
import com.moa.api.repository.writer.PhoneVerificationWriter;
import com.moa.api.repository.writer.SignUpWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.mail.MailMessage;
import com.moa.common.mail.MailSender;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.security.SecureTokens;
import com.moa.common.security.pii.PiiCipher;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.sms.SmsMessage;
import com.moa.common.sms.SmsSender;
import com.moa.common.web.WebProperties;

import lombok.RequiredArgsConstructor;

/**
 * 가입 진행: 이메일 인증(이메일 인증 연결 포함), 휴대폰 인증, 진행 상태, 취소, 만료 정리.
 * 발송 메서드(send*)는 트랜잭션을 열지 않는다. Application이 저장 트랜잭션이 끝난 뒤 부른다.
 */
@Service
@RequiredArgsConstructor
public class SignUpService {

	private static final long DAY_HOURS = 24;

	private final SignUpReader signUpReader;
	private final SignUpWriter signUpWriter;
	private final EmailVerificationReader emailVerificationReader;
	private final EmailVerificationWriter emailVerificationWriter;
	private final PhoneVerificationReader phoneVerificationReader;
	private final PhoneVerificationWriter phoneVerificationWriter;
	private final MemberReader memberReader;
	private final MemberSocialAccountReader memberSocialAccountReader;
	private final MemberSocialAccountWriter memberSocialAccountWriter;
	private final NotificationService notificationService;
	private final AuthTokenService authTokenService;
	private final PasswordEncoder passwordEncoder;
	private final PiiCipher piiCipher;
	private final WebProperties webProperties;
	private final MailSender mailSender;
	private final SmsSender smsSender;

	/**
	 * 유효한 가입 진행을 읽는다.
	 *
	 * @throws ServiceException 토큰이 없거나, 가입 진행이 없거나 만료됐으면 A006
	 */
	@Transactional(readOnly = true)
	public SignUp getValidSignUp(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new ServiceException(ErrorCode.SIGN_UP_REQUIRED);
		}
		return signUpReader.findByTokenHash(SecureTokens.sha256(rawToken))
			.filter(signUp -> !signUp.isExpired(LocalDateTime.now()))
			.orElseThrow(() -> new ServiceException(ErrorCode.SIGN_UP_REQUIRED));
	}

	// 이메일 인증

	/**
	 * @throws ServiceException 비밀번호가 있는 기존 계정 이메일이면 B002, 60초 안에 다시 보내면 B008,
	 *     가입 진행 쿠키가 무효면 A006
	 */
	@Transactional
	public IssuedEmailVerificationDto issueEmailVerification(String rawEmail, String password, String signUpToken) {
		String email = SignUpRules.normalizeEmail(rawEmail);
		Long signUpId = null;
		if (signUpToken != null) {
			SignUp signUp = getValidSignUp(signUpToken);
			if (signUp.awaitsEmail()) {
				signUpId = signUp.getId();
			}
		}
		if (memberReader.findByEmail(email).filter(Member::hasPassword).isPresent()) {
			throw new ServiceException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}
		LocalDateTime now = LocalDateTime.now();
		checkResendInterval(emailVerificationReader.findLatestByEmail(email).map(EmailVerification::getCreatedAt),
			now);
		String rawToken = SecureTokens.newToken();
		EmailVerification verification = emailVerificationWriter.create(new EmailVerification(signUpId, email,
			passwordEncoder.encode(password), SecureTokens.sha256(rawToken),
			now.plus(MemberPolicy.EMAIL_LINK_TTL)));
		return new IssuedEmailVerificationDto(verification.getId(), email, rawToken,
			SignUpRules.toInstant(verification.getExpiresAt()),
			SignUpRules.toInstant(now.plus(MemberPolicy.RESEND_INTERVAL)));
	}

	/**
	 * 인증 메일을 보낸다. 링크는 {@code <웹>/signup/phone?token=<토큰>}이다.
	 *
	 * @throws com.moa.common.mail.MailSendException 발송 실패
	 */
	public void sendVerificationMail(IssuedEmailVerificationDto issued) {
		String link = webProperties.url("/signup/phone?token=" + issued.rawToken());
		mailSender.send(new MailMessage(issued.email(), "[moa] 이메일 인증",
			"moa 가입을 이어 가려면 30분 안에 아래 링크를 여세요. 링크는 한 번만 쓸 수 있습니다.\n\n" + link + "\n\n"
				+ "가입을 요청하지 않았다면 이 메일을 무시하세요."));
	}

	/** 보내지 못한 인증 링크를 지운다. 쓸 수 없게 되고, 다시 보내기 간격에도 세지 않는다 */
	@Transactional
	public void discardEmailVerification(Long id) {
		emailVerificationWriter.delete(id);
	}

	/**
	 * @throws ServiceException 만료·이미 씀·없는 링크면 B004, 비밀번호가 있는 기존 계정 이메일이면 B002,
	 *     기존 계정이 ACTIVE가 아니면 B001, 카카오를 연결할 수 없으면 B003, 이어 온 가입 진행이 만료됐으면 A006
	 */
	@Transactional
	public EmailConfirmationDto confirmEmailVerification(String rawToken) {
		LocalDateTime now = LocalDateTime.now();
		EmailVerification verification = emailVerificationReader
			.findForUpdateByTokenHash(SecureTokens.sha256(rawToken))
			.filter(found -> found.isUsable(now))
			.orElseThrow(() -> new ServiceException(ErrorCode.VERIFICATION_LINK_INVALID));
		verification.markUsed(now);

		Optional<SignUp> kakaoSignUp = Optional.empty();
		if (verification.getSignUpId() != null) {
			kakaoSignUp = Optional.of(signUpReader.findById(verification.getSignUpId())
				.filter(signUp -> !signUp.isExpired(now))
				.orElseThrow(() -> new ServiceException(ErrorCode.SIGN_UP_REQUIRED)));
		}

		Optional<Member> existing = memberReader.findByEmail(verification.getEmail());
		if (existing.isPresent()) {
			return linkToExistingAccount(existing.get(), verification, kakaoSignUp);
		}
		String newToken = SecureTokens.newToken();
		if (kakaoSignUp.isPresent()) {
			kakaoSignUp.get().confirmEmail(verification.getEmail(), verification.getPasswordHash(),
				SecureTokens.sha256(newToken));
		} else {
			signUpWriter.create(SignUp.startWithEmail(SecureTokens.sha256(newToken), verification.getEmail(),
				verification.getPasswordHash(), MemberPolicy.SIGN_UP_TTL));
		}
		return new EmailConfirmationDto(EmailConfirmationResult.CONTINUE_SIGN_UP, newToken, null);
	}

	/** 이메일 인증 연결: 비밀번호를 추가하고, 카카오 가입 중이었으면 카카오도 연결한 뒤 로그인시킨다 */
	private EmailConfirmationDto linkToExistingAccount(Member member, EmailVerification verification,
		Optional<SignUp> kakaoSignUp) {
		if (member.hasPassword()) {
			throw new ServiceException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}
		if (!member.isActive()) {
			throw new ServiceException(ErrorCode.INVALID_STATE);
		}
		member.addPassword(verification.getPasswordHash());
		notificationService.create(member.getId(), NotificationType.LOGIN_METHOD_ADDED, null, null, null);
		if (kakaoSignUp.isPresent()) {
			SignUp signUp = kakaoSignUp.get();
			MemberSocialAccount account = linkSocialAccount(member, signUp.getProvider(), signUp.getProviderUserId());
			notificationService.create(member.getId(), NotificationType.LOGIN_METHOD_LINKED, null,
				NotificationTargetType.MEMBER_SOCIAL_ACCOUNT, account.getId());
			signUpWriter.delete(signUp);
		}
		IssuedTokens tokens = authTokenService.issueWithNewSession(SignUpRules.principalOf(member));
		return new EmailConfirmationDto(EmailConfirmationResult.SIGNED_IN, null, tokens);
	}

	private MemberSocialAccount linkSocialAccount(Member member, OAuthProvider provider, String providerUserId) {
		boolean providerTaken = memberSocialAccountReader.find(provider, providerUserId).isPresent();
		if (providerTaken || memberSocialAccountReader.existsByMemberAndProvider(member.getId(), provider)) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
		}
		return memberSocialAccountWriter.create(new MemberSocialAccount(member.getId(), provider, providerUserId));
	}

	// 휴대폰 인증

	/**
	 * @throws ServiceException 가입 진행이 무효면 A006, 계정 이메일이 아직 없으면 B015, 60초 안에 다시 받으면 B008,
	 *     하루 10회를 넘으면 B009
	 */
	@Transactional
	public IssuedPhoneVerificationDto issuePhoneVerification(String signUpToken, String phoneNumber) {
		SignUp signUp = getValidSignUp(signUpToken);
		if (signUp.getEmail() == null) {
			throw new ServiceException(ErrorCode.SIGN_UP_INCOMPLETE);
		}
		String phoneHmac = piiCipher.hmac(phoneNumber);
		LocalDateTime now = LocalDateTime.now();
		checkResendInterval(phoneVerificationReader.findLatestByPhoneHmac(phoneHmac)
			.map(PhoneVerification::getCreatedAt), now);
		if (phoneVerificationReader.countByPhoneHmacSince(phoneHmac,
			now.minusHours(DAY_HOURS)) >= MemberPolicy.DAILY_SMS_LIMIT) {
			throw new ServiceException(ErrorCode.DAILY_SEND_LIMIT_EXCEEDED);
		}
		String code = SecureTokens.newNumericCode();
		PhoneVerification verification = phoneVerificationWriter.create(new PhoneVerification(signUp.getId(),
			piiCipher.encrypt(phoneNumber), phoneHmac, SecureTokens.sha256(code),
			now.plus(MemberPolicy.PHONE_CODE_TTL)));
		PhoneVerificationDto result = new PhoneVerificationDto(SignUpRules.toInstant(verification.getExpiresAt()),
			SignUpRules.toInstant(now.plus(MemberPolicy.RESEND_INTERVAL)));
		return new IssuedPhoneVerificationDto(verification.getId(), phoneNumber, code, result);
	}

	/**
	 * @throws com.moa.common.sms.SmsSendException 발송 실패
	 */
	public void sendVerificationSms(IssuedPhoneVerificationDto issued) {
		smsSender.send(new SmsMessage(issued.phoneNumber(),
			"[moa] 인증 번호 " + issued.verificationCode() + "를 3분 안에 입력하세요."));
	}

	/** 보내지 못한 인증 번호를 지운다. 쓸 수 없게 되고, 다시 받기 간격과 하루 횟수에도 세지 않는다 */
	@Transactional
	public void discardPhoneVerification(Long id) {
		phoneVerificationWriter.delete(id);
	}

	/**
	 * 인증 번호가 맞으면 번호 주인임이 확인된 것으로 보고 가입 진행에 번호와 생년월일을 둔 뒤 중복을 검사한다.
	 * 입력 실패 횟수와 확인된 번호는 오류로 끝나도 남긴다 (가입 진행 상태 조회가 중복 안내를 준다).
	 *
	 * @throws ServiceException A006, B010 만 14세 미만, B005 번호 틀림, B006 만료·없음, B007 입력 횟수 넘음,
	 *     B011~B014 번호 겹침·재가입 제한
	 */
	@Transactional(noRollbackFor = ServiceException.class)
	public void confirmPhoneVerification(String signUpToken, String verificationCode, LocalDate birthDate) {
		SignUp signUp = getValidSignUp(signUpToken);
		SignUpRules.checkAge(birthDate);
		LocalDateTime now = LocalDateTime.now();
		PhoneVerification verification = phoneVerificationReader.findLatestForUpdateBySignUpId(signUp.getId())
			.filter(found -> !found.isVerified())
			.orElseThrow(() -> new ServiceException(ErrorCode.VERIFICATION_CODE_EXPIRED));
		if (verification.getFailedAttempts() >= MemberPolicy.PHONE_CODE_MAX_ATTEMPTS) {
			throw new ServiceException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
		}
		if (verification.isExpired(now)) {
			throw new ServiceException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}
		if (!SecureTokens.sha256(verificationCode).equals(verification.getCodeHash())) {
			verification.recordFailure();
			throw new ServiceException(ErrorCode.VERIFICATION_CODE_MISMATCH);
		}
		verification.verify(now);
		signUp.verifyPhone(birthDate, verification.getPhoneEncrypted(), verification.getPhoneHmac(), now);
		SignUpRules.checkPhoneOwner(memberReader.findByPhoneHmac(verification.getPhoneHmac()));
	}

	// 진행 상태

	@Transactional(readOnly = true)
	public SignUpDto getSignUp(String rawToken) {
		SignUp signUp = getValidSignUp(rawToken);
		SignUpDto.PhoneConflict conflict = null;
		if (signUp.isPhoneVerified()) {
			conflict = memberReader.findByPhoneHmac(signUp.getPhoneHmac())
				.map(owner -> new SignUpDto.PhoneConflict(owner.getStatus().name(),
					SignUpRules.showsMaskedEmail(owner.getStatus()) && owner.getEmail() != null
						? SignUpRules.maskEmail(owner.getEmail())
						: null))
				.orElse(null);
		}
		return new SignUpDto(signUp.getEmail(), signUp.getProvider(), signUp.isPhoneVerified(), conflict,
			SignUpRules.toInstant(signUp.getExpiresAt()));
	}

	/** 가입 진행을 지운다. 토큰이 없거나 무효여도 실패하지 않는다 */
	@Transactional
	public void cancelSignUp(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		signUpReader.findByTokenHash(SecureTokens.sha256(rawToken)).ifPresent(signUpWriter::delete);
	}

	/**
	 * 만료된 가입 진행과 인증 링크를 지운다. 인증 번호는 하루 발송 횟수를 센 뒤(하루 지난 것) 지운다.
	 */
	@Transactional
	public void deleteExpired() {
		LocalDateTime now = LocalDateTime.now();
		signUpWriter.deleteExpiredBefore(now);
		emailVerificationWriter.deleteExpiredBefore(now);
		phoneVerificationWriter.deleteCreatedBefore(now.minusHours(DAY_HOURS));
	}

	private static void checkResendInterval(Optional<LocalDateTime> lastSentAt, LocalDateTime now) {
		lastSentAt.filter(sentAt -> sentAt.plus(MemberPolicy.RESEND_INTERVAL).isAfter(now))
			.ifPresent(sentAt -> {
				throw new ServiceException(ErrorCode.RESEND_TOO_SOON);
			});
	}
}
