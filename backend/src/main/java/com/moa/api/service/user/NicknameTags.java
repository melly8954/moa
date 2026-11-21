package com.moa.api.service.user;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moa.api.repository.reader.user.UserProfileReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

/**
 * 닉네임 태그 정하기 (specs/009-mypage research R4, specs/010-social-login research R14).
 * 태그는 같은 닉네임 키 안에서 겹치지 않는 0001~9999 중 무작위다. 회원이 고르지 않는다.
 * <p>
 * 고른 뒤 저장할 때까지 다른 요청이 같은 번호를 가져갈 수 있다. 그때는 유일 제약
 * uk_user_profile_nickname_key_nickname_tag가 막으므로, 부르는 쪽(Application)이 {@link #isTagConflict}로 알아보고
 * 새 트랜잭션으로 처음부터 다시 한다 (최대 {@link #MAX_ATTEMPTS}번, 그래도면 B201).
 */
@Component
public class NicknameTags {

	public static final int MAX_ATTEMPTS = 3;
	static final int MAX_TAG = 9999;
	static final String UNIQUE_CONSTRAINT = "uk_user_profile_nickname_key_nickname_tag";
	private static final int RANDOM_TRIES = 32;

	private final UserProfileReader userProfileReader;
	private final RandomGenerator random;

	@Autowired
	public NicknameTags(UserProfileReader userProfileReader) {
		this(userProfileReader, new SecureRandom());
	}

	NicknameTags(UserProfileReader userProfileReader, RandomGenerator random) {
		this.userProfileReader = userProfileReader;
		this.random = random;
	}

	/**
	 * 이 키에서 쓰이지 않은 태그 하나.
	 *
	 * @throws ServiceException 9999개가 모두 쓰이면 B201
	 */
	public String assign(String nicknameKey) {
		Set<String> used = userProfileReader.findUsedTags(nicknameKey);
		if (used.size() >= MAX_TAG) {
			throw new ServiceException(ErrorCode.NICKNAME_TAGS_EXHAUSTED);
		}
		for (int i = 0; i < RANDOM_TRIES; i++) {
			String tag = format(random.nextInt(1, MAX_TAG + 1));
			if (!used.contains(tag)) {
				return tag;
			}
		}
		List<String> free = new ArrayList<>(MAX_TAG - used.size());
		for (int n = 1; n <= MAX_TAG; n++) {
			String tag = format(n);
			if (!used.contains(tag)) {
				free.add(tag);
			}
		}
		if (free.isEmpty()) {
			throw new ServiceException(ErrorCode.NICKNAME_TAGS_EXHAUSTED);
		}
		return free.get(random.nextInt(free.size()));
	}

	/**
	 * 닉네임을 바꿀 때의 태그: 키가 같으면(대소문자만 바뀜) 그대로, 새 키에서 지금 태그가 비어 있으면 그대로, 아니면 새로.
	 *
	 * @param currentKey 지금 키. 프로필이 없으면 null
	 * @param currentTag 지금 태그. 프로필이 없으면 null
	 * @throws ServiceException 새로 정해야 하는데 9999개가 모두 쓰이면 B201
	 */
	public String keepOrAssign(String newKey, String currentKey, String currentTag) {
		if (currentTag == null) {
			return assign(newKey);
		}
		if (newKey.equals(currentKey)) {
			return currentTag;
		}
		if (!userProfileReader.findUsedTags(newKey).contains(currentTag)) {
			return currentTag;
		}
		return assign(newKey);
	}

	/** 저장 중 태그 유일 제약에 걸렸는지 (다시 할지 판단) */
	public static boolean isTagConflict(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains(UNIQUE_CONSTRAINT)) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	static String format(int number) {
		return String.format("%04d", number);
	}
}
