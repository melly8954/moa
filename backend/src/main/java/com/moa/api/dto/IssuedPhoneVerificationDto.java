package com.moa.api.dto;

/**
 * 저장했지만 아직 보내지 않은 인증 번호. 발송은 트랜잭션 밖에서 한다.
 *
 * @param id 휴대폰 인증 ID. 발송에 실패하면 지운다
 * @param phoneNumber 보낼 번호
 * @param verificationCode 인증 번호 원문. 문자 본문에만 넣는다
 * @param result 화면에 줄 만료·다시 받기 시각
 */
public record IssuedPhoneVerificationDto(Long id, String phoneNumber, String verificationCode,
	PhoneVerificationDto result) {
}
