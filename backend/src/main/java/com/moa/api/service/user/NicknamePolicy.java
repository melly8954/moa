package com.moa.api.service.user;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

/**
 * 닉네임 규칙 한 곳 (잠정, specs/009-mypage FR-003·research R4, specs/010-social-login FR-005·research R14).
 * 010 가입, 009 변경이 같이 쓴다. 닉네임은 다른 회원과 겹쳐도 된다 (구별은 NicknameTags).
 * <p>
 * 정규화 순서: NFC 변환 → 앞뒤 공백 제거 → (키만) 소문자. 분해형 한글 입력도 완성형과 같게 본다.
 */
public final class NicknamePolicy {

	public static final String FIELD = "nickname";
	private static final Pattern ALLOWED = Pattern.compile("^[가-힣A-Za-z0-9_]{2,12}$");
	/** 대소문자 무시, 같으면 막는다 (잠정) */
	private static final Set<String> RESERVED = Set.of("admin", "administrator", "root", "system", "moa", "운영자",
		"관리자", "운영팀", "관리팀");

	private NicknamePolicy() {
	}

	/** NFC 변환 → 앞뒤 공백 제거. null은 빈 문자열 */
	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		return Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
	}

	/** 태그 공간을 나누는 키: normalize → 소문자 (tb_user_profile.nickname_key) */
	public static String key(String nickname) {
		return normalize(nickname).toLowerCase(Locale.ROOT);
	}

	/**
	 * 규칙을 확인하고 정규화한 닉네임을 돌려준다.
	 *
	 * @throws ServiceException 비었거나 2~12자 한글·영문·숫자·_가 아니거나 예약어면 V001 (칸 nickname)
	 */
	public static String validate(String raw) {
		return validate(raw, FIELD);
	}

	/**
	 * @param field 칸 오류에 쓸 요청 칸 이름 (관리자 이름 바꾸기는 name)
	 */
	public static String validate(String raw, String field) {
		String nickname = normalize(raw);
		if (nickname.isEmpty()) {
			throw invalid(field, "닉네임을 입력해 주세요.");
		}
		if (!ALLOWED.matcher(nickname).matches()) {
			throw invalid(field, "닉네임은 2~12자의 한글, 영문, 숫자, _만 쓸 수 있습니다.");
		}
		if (RESERVED.contains(key(nickname))) {
			throw invalid(field, "쓸 수 없는 닉네임입니다.");
		}
		return nickname;
	}

	private static ServiceException invalid(String field, String message) {
		return new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
			List.of(new FieldError(field, message)));
	}
}
