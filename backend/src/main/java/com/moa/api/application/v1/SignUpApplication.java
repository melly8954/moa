package com.moa.api.application.v1;

import org.springframework.stereotype.Component;

import com.moa.api.application.v1.command.ConfirmEmailVerificationCommand;
import com.moa.api.application.v1.command.ConfirmPhoneVerificationCommand;
import com.moa.api.application.v1.command.CreateEmailVerificationCommand;
import com.moa.api.application.v1.command.CreatePhoneVerificationCommand;
import com.moa.api.dto.EmailConfirmationDto;
import com.moa.api.dto.EmailVerificationDto;
import com.moa.api.dto.IssuedEmailVerificationDto;
import com.moa.api.dto.IssuedPhoneVerificationDto;
import com.moa.api.dto.PhoneVerificationDto;
import com.moa.api.dto.SignUpDto;
import com.moa.api.service.SignUpService;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.mail.MailSendException;
import com.moa.common.sms.SmsSendException;

import lombok.RequiredArgsConstructor;

/**
 * 가입 진행(이메일 인증, 휴대폰 인증) API의 진입점. 메일·문자 발송은 트랜잭션 밖에서 한다.
 */
@Component
@RequiredArgsConstructor
public class SignUpApplication {

	private final SignUpService signUpService;

	/**
	 * 인증 메일을 보낸다. 링크는 {@code <웹>/signup/phone?token=<토큰>}이다.
	 *
	 * @throws com.moa.common.exception.ServiceException 비밀번호가 있는 기존 계정 이메일이면 B002,
	 *     60초 안에 다시 보내면 B008, 발송 실패면 S002, 가입 진행 토큰이 무효면 A006
	 */
	public EmailVerificationDto createEmailVerification(CreateEmailVerificationCommand command) {
		IssuedEmailVerificationDto issued = signUpService.issueEmailVerification(command.email(), command.password(),
			command.signUpToken());
		try {
			signUpService.sendVerificationMail(issued);
		} catch (MailSendException ex) {
			signUpService.discardEmailVerification(issued.id());
			throw new ServiceException(ErrorCode.MAIL_SEND_FAILED);
		}
		return new EmailVerificationDto(issued.email(), issued.expiresAt(), issued.resendAvailableAt());
	}

	/**
	 * 인증 링크를 확인한다. 같은 이메일의 기존 계정이 있으면 이메일 인증 연결 규칙대로 연결하고 로그인시킨다.
	 * 연결할 때 같은 트랜잭션에서 NotificationService.create로 그 계정에 로그인 수단 변경 알림을 남긴다.
	 * <ul>
	 *   <li>비밀번호 추가: LOGIN_METHOD_ADDED, 대상 없음</li>
	 *   <li>카카오 가입 중이었으면 카카오 연결: LOGIN_METHOD_LINKED, 대상 MEMBER_SOCIAL_ACCOUNT</li>
	 * </ul>
	 *
	 * @throws com.moa.common.exception.ServiceException 만료·이미 씀·없는 링크면 B004,
	 *     비밀번호가 있는 기존 계정 이메일이면 B002
	 */
	public EmailConfirmationDto confirmEmailVerification(ConfirmEmailVerificationCommand command) {
		return signUpService.confirmEmailVerification(command.token());
	}

	/**
	 * 문자 인증 번호를 보낸다.
	 *
	 * @throws com.moa.common.exception.ServiceException 가입 진행 토큰이 무효면 A006, 계정 이메일이 아직 없으면 B015,
	 *     60초 안에 다시 받으면 B008, 하루 10회를 넘으면 B009, 발송 실패면 S003
	 */
	public PhoneVerificationDto createPhoneVerification(CreatePhoneVerificationCommand command) {
		IssuedPhoneVerificationDto issued = signUpService.issuePhoneVerification(command.signUpToken(),
			command.phoneNumber());
		try {
			signUpService.sendVerificationSms(issued);
		} catch (SmsSendException ex) {
			signUpService.discardPhoneVerification(issued.id());
			throw new ServiceException(ErrorCode.SMS_SEND_FAILED);
		}
		return issued.result();
	}

	/**
	 * 인증 번호와 생년월일을 확인한다. 번호가 맞으면 번호 주인임이 확인된 것으로 보고 중복을 검사한다.
	 *
	 * @throws com.moa.common.exception.ServiceException 가입 진행 토큰이 무효면 A006, 번호가 틀리면 B005,
	 *     만료면 B006, 5회를 넘으면 B007, 만 14세 미만이면 B010, 번호가 겹치면 계정 상태에 따라 B011·B012·B013,
	 *     재가입 제한 중이면 B014
	 */
	public void confirmPhoneVerification(ConfirmPhoneVerificationCommand command) {
		signUpService.confirmPhoneVerification(command.signUpToken(), command.verificationCode(),
			command.birthDate());
	}

	/**
	 * @throws com.moa.common.exception.ServiceException 가입 진행 토큰이 무효면 A006
	 */
	public SignUpDto getSignUp(String signUpToken) {
		return signUpService.getSignUp(signUpToken);
	}

	/** 가입을 취소한다. 토큰이 없거나 무효여도 실패하지 않는다 */
	public void cancelSignUp(String signUpToken) {
		signUpService.cancelSignUp(signUpToken);
	}

	/** 만료된 가입 진행과 인증을 지운다. 예약 작업이 부른다 */
	public void deleteExpired() {
		signUpService.deleteExpired();
	}
}
